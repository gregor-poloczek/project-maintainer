package de.gregorpoloczek.projectmaintainer.reporting;

import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.analysis.VersionedLabel;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ColumnProperties;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ReportProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProjectReportGeneratorService {

    private final LabelService labelService;
    private final WorkingCopyService workingCopyService;
    private final ProjectService projectService;
    private final ProjectAnalysisService projectAnalysisService;
    private final ReportingProperties reportingProperties;

    public ProjectReportGeneratorService(
            LabelService labelService,
            WorkingCopyService workingCopyService,
            ProjectService projectService,
            ProjectAnalysisService projectAnalysisService,
            ReportingProperties reportingProperties
    ) {
        this.labelService = labelService;
        this.workingCopyService = workingCopyService;
        this.projectService = projectService;
        this.projectAnalysisService = projectAnalysisService;
        this.reportingProperties = reportingProperties;
    }

    public Mono<Report> getReport(String reportId) {
        Optional<ReportProperties> properties = reportingProperties.getReports().stream()
                .filter(r -> r.getId().equals(reportId))
                .findFirst();

        List<Project> projects = projectService.getProjects()
                .stream()
                .filter(p -> workingCopyService.find(p.getMetaData().getFQPN()).isPresent())
                .toList();

        Flux<Void> analysis = Flux.merge(projects.stream()
                .map(p -> projectAnalysisService.analyze(p.getMetaData().getFQPN())).toList());
        return analysis
                .collectList()
                .flatMap((l) -> {
                    Mono<ReportProperties> mono = Mono.justOrEmpty(properties)
                            .switchIfEmpty(
                                    Mono.error(new IllegalArgumentException(
                                            "Cannot find report definition with id " + reportId)));
                    return mono;
                }).map(p -> {
                    ReportDefinition reportDefinition = new ReportDefinition();
                    reportDefinition.id = p.getId();
                    reportDefinition.name = p.getName();
                    reportDefinition.columns = p.getColumns()
                            .stream()
                            .map(c -> Column.builder().label(c.getName()).build())
                            .toList();

                    Report report = new Report();
                    report.definition = reportDefinition;

                    for (Project project : projects) {
                        Row row = new Row(project);
                        SortedSet<Label> labels = this.labelService.find(project.getMetaData().getFQPN());

                        for (ColumnProperties column : p.getColumns()) {
                            Label label = Label.of(column.getVersionLabelBase());
                            Optional<VersionedLabel> match = labels.stream()
                                    .filter(VersionedLabel.class::isInstance)
                                    .map(VersionedLabel.class::cast)
                                    .filter(vL -> vL.getBase().equals(label))
                                    .findFirst();

                            Cell cell = new Cell();
                            if (match.isPresent()) {
                                cell.value = match.get().getVersion();
                            } else {
                                cell.value = null;
                            }
                            row.cells.add(cell);
                        }

                        if (!row.cells.stream().allMatch(c -> c.value == null)) {
                            report.rows.add(row);
                        }
                    }

                    return report;
                });
    }

    @Getter
    public static class ReportDefinition {

        String id;
        String name;
        List<Column> columns = new ArrayList<>();
    }

    @Getter
    public static class Report {

        ReportDefinition definition;
        List<Row> rows = new ArrayList<>();
    }

    @Builder
    @Getter
    public static class Column {

        String label;
    }

    @Getter
    public static class Cell {

        String value;
    }

    @Getter
    public static class Row {

        Project project;
        List<Cell> cells = new ArrayList<>();

        public Row(Project project) {
            this.project = project;
        }

        Cell getCell(int index) {
            return this.cells.get(index);
        }
    }


}

package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.analysis.VersionedLabel;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ColumnProperties;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ReportProperties;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    public Mono<ProjectReport> generateProjectReport(String reportId) {
        Optional<ReportProperties> maybeProperties = reportingProperties.getReports().stream()
                .filter(r -> r.getId().equals(reportId))
                .findFirst();

        if (maybeProperties.isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "Cannot find report definition with id " + reportId));
        }

        Flux<Project> analyzedProjects = Flux.merge(projectService.getProjects()
                .stream()
                .filter(p -> workingCopyService.find(p.getMetaData().getFQPN()).isPresent())
                .map(p -> projectAnalysisService.analyze(p.getMetaData().getFQPN())
                        .subscribeOn(Schedulers.parallel()).thenReturn(p))
                .toList());
        return analyzedProjects
                .collectList()
                .map(projects -> buildReport(projects, maybeProperties.get()));
    }

    private ProjectReport buildReport(List<Project> projects, ReportProperties properties) {
        ProjectReportDefinition reportDefinition = new ProjectReportDefinition();
        reportDefinition.id = properties.getId();
        reportDefinition.name = properties.getName();
        reportDefinition.columns = properties.getColumns()
                .stream()
                .map(c -> ProjectReportColumn.builder().label(c.getName()).build())
                .toList();

        ProjectReport report = new ProjectReport();
        report.definition = reportDefinition;

        for (Project project : projects) {
            ProjectReportRow row = new ProjectReportRow(project);
            SortedSet<Label> labels = this.labelService.find(project.getMetaData().getFQPN());

            for (ColumnProperties column : properties.getColumns()) {
                Label label = Label.of(column.getVersionLabelBase());
                Optional<VersionedLabel> match = labels.stream()
                        .filter(VersionedLabel.class::isInstance)
                        .map(VersionedLabel.class::cast)
                        .filter(vL -> vL.getBase().equals(label))
                        .findFirst();

                ProjectReportCell cell = new ProjectReportCell();
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
    }


}

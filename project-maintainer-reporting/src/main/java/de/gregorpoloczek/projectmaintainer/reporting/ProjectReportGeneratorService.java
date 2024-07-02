package de.gregorpoloczek.projectmaintainer.reporting;

import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
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

    public ProjectReportGeneratorService(
            LabelService labelService,
            WorkingCopyService workingCopyService,
            ProjectService projectService,
            ProjectAnalysisService projectAnalysisService
    ) {
        this.labelService = labelService;
        this.workingCopyService = workingCopyService;
        this.projectService = projectService;
        this.projectAnalysisService = projectAnalysisService;
    }

    public Mono<Report> getReport(String reportId) {
        Report report = new Report();
        report.id = reportId;

        List<Project> projects = projectService.getProjects()
                .stream()
                .filter(p -> workingCopyService.find(p.getMetaData().getFQPN()).isPresent())
                .toList();

        Flux<Void> flux = Flux.merge(projects.stream()
                .map(p -> projectAnalysisService.analyze(p.getMetaData().getFQPN())).toList());

        flux.blockLast();

        for (Project project : projects) {
            Row row = new Row(project);
            SortedSet<Label> labels = this.labelService.find(project.getMetaData().getFQPN());

            report.rows.add(row);
        }

        return Mono.just(report);
    }

    @Getter
    public static class Report {

        String id;
        String name;
        List<Column> columns = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
    }

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

package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ProjectReportRow {

    Project project;
    List<ProjectReportCell> cells = new ArrayList<>();

    public ProjectReportRow(Project project) {
        this.project = project;
    }

    ProjectReportCell getCell(int index) {
        return this.cells.get(index);
    }
}

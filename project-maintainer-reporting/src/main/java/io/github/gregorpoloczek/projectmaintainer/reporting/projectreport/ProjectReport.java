package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReport {

    ProjectReportDefinition definition;
    List<ProjectReportRow> rows = new ArrayList<>();
}

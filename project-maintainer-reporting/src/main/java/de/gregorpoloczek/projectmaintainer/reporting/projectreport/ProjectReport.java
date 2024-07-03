package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ProjectReport {

    ProjectReportDefinition definition;
    List<ProjectReportRow> rows = new ArrayList<>();
}

package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReportDefinition {

    String id;
    String name;
    List<ProjectReportColumn> columns = new ArrayList<>();
}

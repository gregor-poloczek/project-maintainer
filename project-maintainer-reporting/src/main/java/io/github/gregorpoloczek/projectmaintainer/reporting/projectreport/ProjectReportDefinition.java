package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport;

import io.github.gregorpoloczek.projectmaintainer.reporting.common.ReportColumn;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReportDefinition {

    String id;
    String name;
    List<ReportColumn> columns = new ArrayList<>();
}

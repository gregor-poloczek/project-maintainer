package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import io.github.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;

public class Test {

    void a() {
        ProjectReportConfig config =
                ProjectReportBuilder.builder()
                        .column(c -> c.labelBase())
                        .build();
    }

}

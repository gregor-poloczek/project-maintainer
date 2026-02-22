package io.github.gregorpoloczek.projectmaintainer.reporting.development;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import io.github.gregorpoloczek.projectmaintainer.reporting.config.ReportConfig;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent.ProjectReportBuilder;
import io.github.gregorpoloczek.projectmaintainer.reporting.spi.ReportDefinitionProvider;

import java.util.List;

public class JavaVersionTestReport implements ReportDefinitionProvider {

    @Override
    public List<ReportConfig> create() {
        return List.of(ProjectReportBuilder.builder()
                .id("project-maintainer:java-version-test-report")
                .name("Java Version")
                .column(c -> c
                        .name("Java Version")
                        .textAlignment(ColumnTextAlignment.CENTER)
                        .labelBase(Label.fromString("lang:java")))
                .build()
        );
    }

}

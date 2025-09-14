package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import io.github.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;

import java.util.function.Consumer;

public class ProjectReportBuilder {


    public static ProjectReportBuilder builder() {
        return new ProjectReportBuilder();
    }

    public ProjectReportBuilder rowsRequire(Consumer<RowRequirementsBuilder> builderConsumer) {
        return this;
    }

    public ProjectReportBuilder column(Consumer<ProjectReportColumnBuilder> builderConsumer) {
        builderConsumer.accept(new ProjectReportColumnBuilder());
        return this;
    }


    public ProjectReportConfig build() {
        return new ProjectReportConfig();
    }
}

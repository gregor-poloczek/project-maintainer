package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import io.github.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.Consumer;

public class ProjectReportBuilder {

    ProjectReportConfig config = new ProjectReportConfig();

    public static ProjectReportBuilder builder() {
        return new ProjectReportBuilder();
    }

    public ProjectReportBuilder rowsRequire(
            Consumer<RowRequirementsBuilder> builderConsumer) {
        throw new NotImplementedException("");
    }

    public ProjectReportBuilder column(Consumer<ProjectReportColumnBuilder> builderConsumer) {
        ProjectReportColumnBuilder columnBuilder = new ProjectReportColumnBuilder();
        builderConsumer.accept(columnBuilder);
        this.config.getColumns().add(columnBuilder.build());
        return this;
    }


    public ProjectReportBuilder id(String id) {
        config.setId(id);
        return this;
    }

    public ProjectReportBuilder name(String name) {
        config.setName(name);
        return this;
    }

    public ProjectReportConfig build() {
        return config;
    }
}

package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import io.github.gregorpoloczek.projectmaintainer.reporting.config.ColumnConfig;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.Consumer;

public class ProjectReportColumnBuilder {

    private ColumnConfig config = new ColumnConfig();

    public ProjectReportColumnBuilder labelBase(Label labelBase) {
        this.config.setLabelBase(labelBase.getValue());
        return this;
    }

    public ProjectReportColumnBuilder labelPresence(Consumer<LabelMatcherBuilder> builderConsumer) {
        throw new NotImplementedException("");
    }

    public ProjectReportColumnBuilder name(String name) {
        this.config.setName(name);
        return this;
    }

    public ProjectReportColumnBuilder textAlignment(ColumnTextAlignment columnTextAlignment) {
        // TODO [Reporting] use actional constants
        this.config.setTextAlignment(columnTextAlignment.name().toLowerCase());
        return this;
    }

    public ColumnConfig build() {
        return config;
    }
}

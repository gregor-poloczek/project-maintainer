package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.label.Label;

import java.util.function.Consumer;

public class ProjectReportColumnBuilder {

    public ProjectReportColumnBuilder labelBase(Label labelBase) {
        return this;
    }

    public ProjectReportColumnBuilder labelPresence(Consumer<LabelMatcherBuilder> builderConsumer) {
        return this;
    }
}

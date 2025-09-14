package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import java.util.function.Consumer;

public class RowRequirementsBuilder {

    RowRequirementsBuilder labelIsPresent(Consumer<LabelMatcherBuilder> builderConsumer) {
        return this;
    }
}

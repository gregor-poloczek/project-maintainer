package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import org.intellij.lang.annotations.Language;

public class LabelMatcherBuilder {

    public LabelMatcherBuilder exact(Label label) {
        return this;
    }

    public LabelMatcherBuilder withPattern(@Language("regexp") String regexp) {
        return this;
    }
}

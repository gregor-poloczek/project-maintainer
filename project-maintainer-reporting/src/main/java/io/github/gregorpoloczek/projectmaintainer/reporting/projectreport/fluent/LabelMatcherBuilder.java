package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.fluent;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import io.github.gregorpoloczek.projectmaintainer.reporting.config.ColumnConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LabelMatcherBuilder {

    final ColumnConfig columnConfig;


    public LabelMatcherBuilder exact(Label label) {
        columnConfig.setLabelPresence(label.getValue());
        return this;
    }
}

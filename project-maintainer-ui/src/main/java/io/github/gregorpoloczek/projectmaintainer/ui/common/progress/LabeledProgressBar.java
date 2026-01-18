package io.github.gregorpoloczek.projectmaintainer.ui.common.progress;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.dom.Style.WhiteSpace;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@StyleSheet("./styles/common/progress/LabeledProgressBar.css")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LabeledProgressBar extends HorizontalLayout {

    ProgressBar progressBar;
    Div percentage = new Div("");
    Div label = new Div("");

    public LabeledProgressBar() {
        this.addClassName(LabeledProgressBar.class.getSimpleName());
        this.label.getStyle().setWhiteSpace(WhiteSpace.NOWRAP);
        this.label.addClassName("label");
        this.label.setVisible(false);
        this.progressBar = new ProgressBar();
        this.progressBar.setMax(1);
        this.progressBar.setValue(0);
        this.progressBar.setWidthFull();
        this.percentage.getStyle().setWhiteSpace(WhiteSpace.NOWRAP);

        this.setPadding(false);
        this.setAlignItems(Alignment.CENTER);
        this.add(this.label, progressBar, percentage);
    }

    public void setLabel(String label) {
        this.label.setText(label);
        this.label.setVisible(StringUtils.isNotBlank(label));
    }

    public void setValue(double value) {
        double actualValue;
        if (value < 0) {
            log.warn("Value {} is negative, not allowed", value);
            actualValue = 0;
        } else if (value > 1.0) {
            log.warn("Value {} is too big, not allowed", value);
            actualValue = 1.0;
        } else {
            actualValue = value;
        }
        this.progressBar.setValue(actualValue);
        this.percentage.setText(String.format("%.2f %%", actualValue * 100.0d));
    }

}

package io.github.gregorpoloczek.projectmaintainer.ui.common.progress;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;

public class GenericOperationProgressBar extends HorizontalLayout {

    private final LabeledProgressBar progressBar;
    private double progress = 0.0d;

    public GenericOperationProgressBar() {
        this.setPadding(false);

        this.progressBar = new LabeledProgressBar();
        this.progressBar.setWidthFull();

        this.setVisible(false);

        this.add(progressBar);
    }

    public void start() {
        this.progress = 0.0d;
        this.setVisible(true);
        this.updateComponents();
    }

    public void update(OperationProgress<?> progress) {
        this.progress = progress.getProgressRelative();
        this.updateComponents();
    }

    private void updateComponents() {
        double percent = this.progress();
        this.progressBar.setValue(percent);
    }

    public void setLabel(String label) {
        this.progressBar.setLabel(label);
    }

    public void stop() {
        this.setVisible(false);
    }

    private double progress() {
        return this.progress;
    }

}

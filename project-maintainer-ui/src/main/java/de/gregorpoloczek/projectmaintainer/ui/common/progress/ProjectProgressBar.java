package de.gregorpoloczek.projectmaintainer.ui.common.progress;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProjectProgressBar extends HorizontalLayout {

    private final LabeledProgressBar progressBar;

    public ProjectProgressBar() {
        this.setPadding(false);

        this.progressBar = new LabeledProgressBar();
        this.progressBar.setWidthFull();

        this.setVisible(false);

        this.add(progressBar);
    }

    private final Map<FQPN, Double> progress = new HashMap<>();

    public void start(Collection<? extends ProjectRelatable> relatables) {
        this.progress.clear();
        relatables.forEach(r -> this.progress.put(r.getFQPN(), 0.0d));
        this.setVisible(true);
        this.updateComponents();
    }

    public void start(Collection<? extends ProjectRelatable> relatables, String label) {
        start(relatables);
        this.progressBar.setLabel(label);
    }

    public void update(ProjectOperationProgress<?> progress) {
        this.progress.put(progress.getFQPN(), progress.getProgressRelative());
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
        if (progress.isEmpty()) {
            return 1.0d;
        }
        return progress.values()
                .stream().mapToDouble(Double::doubleValue).sum() / progress.size();
    }

}

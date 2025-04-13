package de.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;
import java.text.MessageFormat;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OperationProgressComponent extends FlexLayout {

    private final Div text;
    private final ProgressBar progressBar;
    private final Div value;

    public OperationProgressComponent(Composable<?, ?> composable) {
        text = new Div();
        value = new Div();

        progressBar = new ProgressBar();

        FlexLayout top = new FlexLayout();
        top.setWidth("100%");
        top.setJustifyContentMode(JustifyContentMode.BETWEEN);
        top.setFlexDirection(FlexDirection.ROW);
        top.add(text, value);

        this.add(top, progressBar);
        this.setFlexDirection(FlexDirection.COLUMN);

        this.update(composable);
    }

    public static <C extends Composable<?, C>> Renderer<C> getRenderer() {
        return new ComponentRenderer<>(OperationProgressComponent::new,
                (component, composable) -> ((OperationProgressComponent) component).update(composable));
    }

    public OperationProgressComponent update(Composable<?, ?> composable) {
        Optional<OperationProgress<?>> maybeProgress = composable.requireTrait(HasOperationProgress.class)
                .getOperationProgress();

        if (maybeProgress.isEmpty()) {
            this.progressBar.setVisible(false);
            this.text.setVisible(false);
            this.value.setVisible(false);
            return this;
        }
        this.progressBar.setVisible(true);
        this.text.setVisible(true);
        this.value.setVisible(true);

        OperationProgress<?> progress = maybeProgress.get();
        text.setText(progress.getMessage());

        double progressRelative = progress.getProgressRelative();
        if (Double.isNaN(progressRelative) || Double.isInfinite(progressRelative) || progressRelative > 1.0d) {
            log.warn("Operation progress has an invalid relative progress {}. Coercing back to 0.",
                    progressRelative);
            progressRelative = 0.0d;
        }
        value.setText(
                MessageFormat.format("{0,number,#.#}%",
                        progressRelative * 100));
        progressBar.setValue(progressRelative);
        progressBar.setIndeterminate(false);

        progressBar.removeThemeVariants(
                ProgressBarVariant.LUMO_SUCCESS,
                ProgressBarVariant.LUMO_ERROR,
                ProgressBarVariant.LUMO_CONTRAST);
        if (progress.getState() == OperationProgress.State.DONE) {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
        } else if (progress.getState() == OperationProgress.State.FAILED) {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_ERROR);
        } else {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_CONTRAST);
        }
        return this;
    }
}

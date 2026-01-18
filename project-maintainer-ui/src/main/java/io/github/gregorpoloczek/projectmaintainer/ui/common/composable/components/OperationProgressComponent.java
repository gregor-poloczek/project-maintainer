package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.dom.Style.WhiteSpace;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;

import java.text.MessageFormat;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@StyleSheet("./styles/common/composable/components/OperationProgressComponent.css")
@Slf4j
public class OperationProgressComponent extends FlexLayout {

    private final Div text;
    private final ProgressBar progressBar;
    private final Div value;
    private final Div errorMessage;

    public OperationProgressComponent(Composable<?, ?> composable) {
        this.addClassName(OperationProgressComponent.class.getSimpleName());
        text = new Div();
        value = new Div();
        errorMessage = new Div();
        errorMessage.getStyle().setWhiteSpace(WhiteSpace.PRE_WRAP);

        progressBar = new ProgressBar();

        FlexLayout top = new FlexLayout();
        top.setWidth("100%");
        top.setJustifyContentMode(JustifyContentMode.BETWEEN);
        top.setFlexDirection(FlexDirection.ROW);
        top.add(text, value);

        this.add(top, progressBar, errorMessage);
        this.setFlexDirection(FlexDirection.COLUMN);

        this.update(composable);
    }

    public static <C extends Composable<?, C>> Renderer<C> getRenderer() {
        return new ComponentRenderer<>(OperationProgressComponent::new,
                (component, composable) -> ((OperationProgressComponent) component).update(composable));
    }

    public OperationProgressComponent update(Composable<?, ?> composable) {
        Optional<OperationProgress<?>> maybeProgress = composable
                .requireTrait(HasOperationProgress.class)
                .getOperationProgress();

        VaadinUtils.hide(errorMessage);
        if (maybeProgress.isEmpty()) {
            VaadinUtils.hide(progressBar, text, value);
            return this;
        }
        OperationProgress<?> progress = maybeProgress.get();
        VaadinUtils.toggleClassName(this, progress.getThrowable().isPresent(), "has-error");

        if (progress.getThrowable().isPresent()) {
            VaadinUtils.hide(progressBar, text, value);
            VaadinUtils.show(errorMessage);
            errorMessage.setText(progress.getThrowable().get().getMessage());
            return this;
        }

        VaadinUtils.show(progressBar, text, value);

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
        text.setText(progress.getMessage());

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

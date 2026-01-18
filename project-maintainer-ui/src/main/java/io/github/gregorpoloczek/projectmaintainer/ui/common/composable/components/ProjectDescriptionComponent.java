package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;

import java.util.Objects;

public class ProjectDescriptionComponent extends FlexLayout {

    private final Div text;
    private String description;

    public static <C extends Composable<?, C>> Renderer<C> getRenderer() {
        return new ComponentRenderer<>(ProjectDescriptionComponent::new,
                ((component, composable) -> ((ProjectDescriptionComponent) component).update(composable)));
    }


    public ProjectDescriptionComponent(Composable<?, ?> composable) {
        text = new Div();
        text.getStyle().set("text-wrap", "balance");
        this.add(text);

        this.update(composable);
    }

    public ProjectDescriptionComponent update(Composable<?, ?> composable) {
        String newDescription = composable.requireTrait(HasProject.class)
                .getProject()
                .getMetaData()
                .getDescription()
                .orElse("");

        if (Objects.equals(this.description, newDescription)) {
            return this;
        }
        this.description = newDescription;
        this.text.setText(newDescription);
        return this;
    }
}

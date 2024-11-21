package de.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProjectNameComponent extends FlexLayout {

    public static <C extends Composable<C>> Renderer<C> getRenderer() {
        return new ComponentRenderer<>(ProjectNameComponent::new,
                (component, composable) -> ((ProjectNameComponent) component).update(composable));
    }

    public ProjectNameComponent(Composable<?> composable) {
        HorizontalLayout badges = new HorizontalLayout();

        HasText name;
        Project project = composable.requireTrait(HasProject.class).getProject();
        Optional<String> maybeBrowserLink = project.getMetaData().getBrowserLink();
        if (maybeBrowserLink.isPresent()) {
            Anchor anchor = new Anchor();
            anchor.setHref(maybeBrowserLink.get());
            anchor.setTarget("_blank");
            name = anchor;
        } else {
            name = new Text("");
        }
        name.setText(project.getMetaData().getName());

        Span prefix = createBadge();
        prefix.setText(getNamePrefix(project));
        badges.add(prefix);
        Div spacer = new Div();
        spacer.getStyle().set("height", "4px");
        this.add(badges, spacer, (Component) name);

        this.setFlexDirection(FlexDirection.COLUMN);
    }

    private Span createBadge() {
        Span badge = new Span("");
        badge.getElement().getThemeList().add("badge");
        return badge;
    }

    private String getNamePrefix(Project project) {
        return project.getMetaData().getFQPN().getSegments()
                .stream()
                .skip(1)
                .filter(s -> !s.equals(project.getMetaData().getName()))
                .collect(Collectors.joining(" / "));
    }

    public ProjectNameComponent update(Composable<?> composable) {
        // nothing to update
        return this;
    }


}

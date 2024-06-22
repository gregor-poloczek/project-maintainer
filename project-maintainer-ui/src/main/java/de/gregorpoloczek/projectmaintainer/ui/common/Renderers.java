package de.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Renderers {

    public interface HasProjectItem {

        Project getProject();
    }

    private static Span createBadge() {
        Span badge = new Span("");
        badge.getElement().getThemeList().add("badge");
        return badge;
    }

    public <I extends HasProjectItem> Renderer<I> getNameRenderer() {
        return new ComponentRenderer<>((I item) -> {
            FlexLayout layout = new FlexLayout();
            HorizontalLayout badges = new HorizontalLayout();
            layout.setFlexDirection(FlexDirection.COLUMN);

            Component name;
            Project project = item.getProject();
            if (project.getMetaData().getBrowserLink().isPresent()) {
                Anchor anchor = new Anchor();
                anchor.setHref(project.getMetaData().getBrowserLink().get());
                anchor.setTarget("_blank");
                name = anchor;
            } else {
                name = new Text("");
            }
            ((HasText) name).setText(project.getMetaData().getName());

            Span prefix = createBadge();
            prefix.setText(getNamePrefix(project));
            badges.add(prefix);
            layout.add(badges, name);
            return layout;
        });
    }

    private static String getNamePrefix(Project project) {
        return project.getMetaData().getFQPN().getSegments()
                .stream()
                .skip(1)
                .filter(s -> !s.equals(project.getMetaData().getName()))
                .collect(Collectors.joining(" / "));
    }

}

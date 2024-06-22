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
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Renderers {

    public interface HasProjectItem {

        Project getProject();
    }

    public interface HasIconItem {

        boolean isIconBlurred();

        Optional<Image> getIcon();
    }

    private static Span createBadge() {
        Span badge = new Span("");
        badge.getElement().getThemeList().add("badge");
        return badge;
    }

    public <I extends HasIconItem> Renderer<I> getIconRenderer() {
        return LitRenderer.<I>of(
                        "<img src=${item.image} style=\"height:32px; filter: grayscale(${item.grayscale});\" />")
                .withProperty("grayscale", item -> !item.isIconBlurred() ? "0.0" : "1.0")
                .withProperty("image", item -> {
                    Optional<Image> image = item.getIcon();
                    return image.map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                            .encodeToString(i.getBytes())).orElse("");
                });

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

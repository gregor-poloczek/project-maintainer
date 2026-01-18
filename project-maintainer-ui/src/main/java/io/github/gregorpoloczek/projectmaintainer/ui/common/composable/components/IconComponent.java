package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;

import java.util.Base64;
import java.util.Optional;

public class IconComponent extends Image {

    public static <C extends Composable<?, C>> Renderer<C> getRenderer() {
        return new ComponentRenderer<>(IconComponent::new,
                (component, composable) -> ((IconComponent) component).update(composable));
    }

    public IconComponent(Composable<?, ?> composable) {
        this.getStyle().setHeight("48px");
        this.update(composable);
    }

    private IconComponent update(Composable<?, ?> composable) {
        HasIcon hasIcon = composable.requireTrait(HasIcon.class);
        // TODO only update if necessary

        Optional<ImageResolverService.Image> image = composable.requireTrait(HasIcon.class).getIcon();

        // "blurring"
        this.getStyle().set("filter", "grayscale(%f)".formatted(hasIcon.isBlurred() ? 1.0 : 0.0));

        String src = image.map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                .encodeToString(i.getBytes())).orElse("");
        this.setSrc(src);

        return this;
    }
}

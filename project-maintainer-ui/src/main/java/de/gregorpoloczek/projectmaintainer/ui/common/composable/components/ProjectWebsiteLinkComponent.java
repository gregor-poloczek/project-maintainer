package de.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import org.apache.commons.lang3.StringUtils;

public class ProjectWebsiteLinkComponent extends FlexLayout {

    private final Anchor anchor;

    public static <C extends Composable<?, C>> Renderer<C> getRenderer() {
        return new ComponentRenderer<>(ProjectWebsiteLinkComponent::new,
                (component, composable) -> ((ProjectWebsiteLinkComponent) component).update(composable));
    }


    public ProjectWebsiteLinkComponent(Composable<?, ?> composable) {
        anchor = new Anchor();
        anchor.add(VaadinIcon.GLOBE_WIRE.create());
        anchor.setTarget("_blank");
        anchor.setHref("");
        anchor.setVisible(false);

        this.add(anchor);

        this.update(composable);
    }

    public ProjectWebsiteLinkComponent update(Composable<?, ?> composable) {
        composable.requireTrait(HasProject.class)
                .getProject()
                .getMetaData()
                .getWebsiteLink()
                .filter(StringUtils::isNotBlank)
                .ifPresent(this.anchor::setHref);
        return this;
    }


}

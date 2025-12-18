package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common;

import com.vaadin.flow.component.html.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;

import java.util.Base64;

public class GitProviderIconComponent extends Image {
    public GitProviderIconComponent(ImageResolverService imageResolverService, String type) {
        String src = imageResolverService.getImage(ImageResolverService.GIT_PROVIDER_GROUP, type)
                .map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                        .encodeToString(i.getBytes())).orElse("");
        this.setHeight("32px");
        this.setSrc(src);
    }
}

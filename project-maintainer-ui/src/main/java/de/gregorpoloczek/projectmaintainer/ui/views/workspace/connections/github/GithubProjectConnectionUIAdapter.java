package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.github;

import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.github.GithubProjectConnection;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.ProjectConnectionUIAdapter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GithubProjectConnectionUIAdapter implements ProjectConnectionUIAdapter<GithubProjectConnection, GithubProjectConnectionFormComponent> {

    ImageResolverService imageResolverService;

    @Override
    public boolean supports(String type) {
        return type.equals(GithubProjectConnection.TYPE);
    }

    @Override
    public GithubProjectConnectionFormComponent createComponent() {
        return new GithubProjectConnectionFormComponent(imageResolverService);
    }

    @Override
    public String getTitle() {
        return "Github";
    }

    @Override
    public String getType() {
        return GithubProjectConnection.TYPE;
    }
}

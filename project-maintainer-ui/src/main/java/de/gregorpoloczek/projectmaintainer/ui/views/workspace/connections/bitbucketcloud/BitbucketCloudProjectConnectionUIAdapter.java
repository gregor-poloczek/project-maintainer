package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.bitbucketcloud;

import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.BitbucketCloudProjectConnection;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.ProjectConnectionUIAdapter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BitbucketCloudProjectConnectionUIAdapter implements ProjectConnectionUIAdapter<BitbucketCloudProjectConnection, BitbucketCloudProjectConnectionFormComponent> {

    ImageResolverService imageResolverService;

    @Override
    public String getType() {
        return BitbucketCloudProjectConnection.TYPE;
    }

    @Override
    public boolean supports(String type) {
        return type.equals(BitbucketCloudProjectConnection.TYPE);
    }

    @Override
    public BitbucketCloudProjectConnectionFormComponent createComponent() {
        return new BitbucketCloudProjectConnectionFormComponent(imageResolverService);
    }

    @Override
    public String getTitle() {
        return "Bitbucket Cloud";
    }

}

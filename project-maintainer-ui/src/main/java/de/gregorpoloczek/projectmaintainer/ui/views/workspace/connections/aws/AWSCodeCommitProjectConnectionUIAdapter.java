package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.aws;

import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.aws.AWSCodeCommitProjectConnection;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.ProjectConnectionUIAdapter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AWSCodeCommitProjectConnectionUIAdapter implements ProjectConnectionUIAdapter<AWSCodeCommitProjectConnection, AWSCodeCommitProjectConnectionFormComponent> {

    ImageResolverService imageResolverService;

    @Override
    public AWSCodeCommitProjectConnectionFormComponent createComponent() {
        return new AWSCodeCommitProjectConnectionFormComponent(imageResolverService);
    }

    @Override
    public String getTitle() {
        return "AWS CodeCommit";
    }

    @Override
    public String getType() {
        return AWSCodeCommitProjectConnection.TYPE;
    }
}

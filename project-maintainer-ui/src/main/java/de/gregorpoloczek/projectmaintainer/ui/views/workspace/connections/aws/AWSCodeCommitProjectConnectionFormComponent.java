package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.aws;

import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.aws.AWSCodeCommitProjectConnection;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.AbstractGenericProjectConnectionFormComponent;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

public class AWSCodeCommitProjectConnectionFormComponent extends AbstractGenericProjectConnectionFormComponent<AWSCodeCommitProjectConnection> {

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String REGION = "region";
    public static final String PROFILE = "profile";

    public AWSCodeCommitProjectConnectionFormComponent(ImageResolverService imageResolverService) {
        super(imageResolverService, AWSCodeCommitProjectConnection.TYPE);
    }

    @Override
    protected List<FieldDefinition> getFieldDefinition() {
        return List.of(FieldDefinition.builder()
                        .type(FieldDefinition.Type.STRING)
                        .id(USERNAME)
                        .defaultValue("")
                        .label("Username").build(),
                FieldDefinition.builder()
                        .type(FieldDefinition.Type.SECRET_STRING)
                        .id(PASSWORD)
                        .defaultValue("")
                        .label("Password").build(),
                FieldDefinition.builder()
                        .type(FieldDefinition.Type.SECRET_STRING)
                        .id(PROFILE)
                        .defaultValue("")
                        .label("Profile").build(),
                FieldDefinition.builder()
                        .type(FieldDefinition.Type.STRING)
                        .id(REGION)
                        .defaultValue("")
                        .label("Region").build()
        );

    }

    @Override
    protected Map<String, Object> extractData(AWSCodeCommitProjectConnection connection) {
        return Map.of(
                USERNAME, connection.getUsername(),
                PASSWORD, connection.getPassword(),
                PROFILE, connection.getProfile(),
                REGION, connection.getRegion()
        );
    }

    @Override
    protected @NonNull AWSCodeCommitProjectConnection createConnection(String id, Map<String, Object> data) {
        return new AWSCodeCommitProjectConnection(id,
                (String) data.get(USERNAME),
                (String) data.get(PASSWORD),
                (String) data.get(PROFILE),
                (String) data.get(REGION));
    }

    protected @NonNull String getTitle() {
        return "AWS CodeCommit";
    }

}

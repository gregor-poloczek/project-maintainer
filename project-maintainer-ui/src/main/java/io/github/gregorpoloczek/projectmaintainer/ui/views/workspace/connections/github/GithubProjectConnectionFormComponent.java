package io.github.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.github;

import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.github.GithubProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import io.github.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.AbstractGenericProjectConnectionFormComponent;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

public class GithubProjectConnectionFormComponent extends AbstractGenericProjectConnectionFormComponent<GithubProjectConnection> {

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public GithubProjectConnectionFormComponent(ImageResolverService imageResolverService) {
        super(imageResolverService, GithubProjectConnection.TYPE);
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
                        .label("Password").build());

    }

    @Override
    protected Map<String, Object> extractData(GithubProjectConnection connection) {
        return Map.of(USERNAME, connection.getUsername(), PASSWORD, connection.getPassword());
    }

    @Override
    protected @NonNull GithubProjectConnection createConnection(String id, Map<String, Object> data) {
        return new GithubProjectConnection(id, (String) data.get(USERNAME), (String) data.get(PASSWORD));
    }

    protected @NonNull String getTitle() {
        return "Github";
    }

}

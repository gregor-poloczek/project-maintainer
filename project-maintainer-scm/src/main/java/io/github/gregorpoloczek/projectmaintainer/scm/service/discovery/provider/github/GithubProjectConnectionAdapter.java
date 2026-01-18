package io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gregorpoloczek.projectmaintainer.core.domain.encryption.service.SecretString;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnectionAdapter;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.UnsupportedProjectConnectionVersionException;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceFileV1;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class GithubProjectConnectionAdapter implements ProjectConnectionAdapter<GithubProjectConnection> {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class GithubProjectConnectionSettingsV1 {
        public static String VERSION = "1";
        @NonNull
        SecretString username;
        @NonNull
        SecretString password;
    }

    public GithubProjectConnection convert(ObjectMapper objectMapper, WorkspaceFileV1.ProjectConnectionV1 projectConnectionV1) throws UnsupportedProjectConnectionVersionException {
        final String id = projectConnectionV1.getId();
        final String type = projectConnectionV1.getType();
        final String version = projectConnectionV1.getVersion();

        if (version.equals(GithubProjectConnectionSettingsV1.VERSION)) {
            final GithubProjectConnectionSettingsV1 settings = objectMapper.convertValue(projectConnectionV1.getSettings(), GithubProjectConnectionSettingsV1.class);
            return new GithubProjectConnection(
                    id,
                    settings.getUsername().getValue(),
                    settings.getPassword().getValue());
        }

        throw new UnsupportedProjectConnectionVersionException(type, version);
    }

    public WorkspaceFileV1.ProjectConnectionV1 convert(ObjectMapper objectMapper, GithubProjectConnection projectConnection) {
        GithubProjectConnectionSettingsV1 githubProjectConnectionSettingsV1 = new GithubProjectConnectionSettingsV1(
                new SecretString(projectConnection.getUsername()),
                new SecretString(projectConnection.getPassword()));
        return WorkspaceFileV1.ProjectConnectionV1.builder()
                .id(projectConnection.getId())
                .type(projectConnection.getType())
                .version(GithubProjectConnectionSettingsV1.VERSION)
                .settings(objectMapper.valueToTree(githubProjectConnectionSettingsV1)).build();
    }

    @Override
    public boolean supports(String type) {
        return type.equals(GithubProjectConnection.TYPE);
    }
}

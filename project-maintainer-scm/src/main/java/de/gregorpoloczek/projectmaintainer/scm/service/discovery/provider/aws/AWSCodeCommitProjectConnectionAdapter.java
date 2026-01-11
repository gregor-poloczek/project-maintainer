package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.domain.encryption.service.SecretString;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnectionAdapter;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.UnsupportedProjectConnectionVersionException;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceFileV1;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AWSCodeCommitProjectConnectionAdapter implements ProjectConnectionAdapter<AWSCodeCommitProjectConnection> {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class AWSCodeCommitProjectConnectionSettingsV1 {
        public static String VERSION = "1";
        @NonNull
        SecretString username;
        @NonNull
        SecretString password;
        @NonNull
        SecretString profile;
        @NonNull
        String region;
    }

    public AWSCodeCommitProjectConnection convert(ObjectMapper objectMapper, WorkspaceFileV1.ProjectConnectionV1 projectConnectionV1) throws UnsupportedProjectConnectionVersionException {
        final String id = projectConnectionV1.getId();
        final String type = projectConnectionV1.getType();
        final String version = projectConnectionV1.getVersion();

        if (version.equals(AWSCodeCommitProjectConnectionSettingsV1.VERSION)) {
            final AWSCodeCommitProjectConnectionSettingsV1 settings = objectMapper.convertValue(projectConnectionV1.getSettings(), AWSCodeCommitProjectConnectionSettingsV1.class);
            return new AWSCodeCommitProjectConnection(
                    id,
                    settings.getUsername().getValue(),
                    settings.getPassword().getValue(),
                    settings.getProfile().getValue(),
                    settings.getRegion()
            );
        }

        throw new UnsupportedProjectConnectionVersionException(type, version);
    }

    public WorkspaceFileV1.ProjectConnectionV1 convert(ObjectMapper objectMapper, AWSCodeCommitProjectConnection projectConnection) {
        AWSCodeCommitProjectConnectionSettingsV1 settings =
                AWSCodeCommitProjectConnectionSettingsV1.builder()
                        .username(new SecretString(projectConnection.getUsername()))
                        .password(new SecretString(projectConnection.getPassword()))
                        .profile(new SecretString(projectConnection.getProfile()))
                        .region(projectConnection.getRegion())
                        .build();

        return WorkspaceFileV1.ProjectConnectionV1.builder()
                .id(projectConnection.getId())
                .type(projectConnection.getType())
                .version(AWSCodeCommitProjectConnectionSettingsV1.VERSION)
                .settings(objectMapper.valueToTree(settings)).build();
    }

    @Override
    public boolean supports(String type) {
        return type.equals(AWSCodeCommitProjectConnection.TYPE);
    }
}

package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket;

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
public class BitbucketCloudProjectConnectionAdapter implements ProjectConnectionAdapter<BitbucketCloudProjectConnection> {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class BitbucketCloudProjectConnectionSettingsV1 {
        public static String VERSION = "1";
        @NonNull
        SecretString email;
        @NonNull
        SecretString bitbucketUsername;
        @NonNull
        SecretString password;
    }

    public BitbucketCloudProjectConnection convert(ObjectMapper objectMapper, WorkspaceFileV1.ProjectConnectionV1 projectConnectionV1) throws UnsupportedProjectConnectionVersionException {
        final String id = projectConnectionV1.getId();
        final String type = projectConnectionV1.getType();
        final String version = projectConnectionV1.getVersion();

        if (version.equals(BitbucketCloudProjectConnectionSettingsV1.VERSION)) {
            final BitbucketCloudProjectConnectionSettingsV1 settings = objectMapper.convertValue(projectConnectionV1.getSettings(), BitbucketCloudProjectConnectionSettingsV1.class);
            return new BitbucketCloudProjectConnection(
                    id,
                    settings.getEmail().getValue(),
                    settings.getBitbucketUsername().getValue(),
                    settings.getPassword().getValue());
        }

        throw new UnsupportedProjectConnectionVersionException(type, version);
    }

    public WorkspaceFileV1.ProjectConnectionV1 convert(ObjectMapper objectMapper, BitbucketCloudProjectConnection projectConnection) {
        BitbucketCloudProjectConnectionSettingsV1 bitbucketCloudProjectConnectionSettingsV1 =
                BitbucketCloudProjectConnectionSettingsV1.builder()
                        .email(new SecretString(projectConnection.getEmail()))
                        .bitbucketUsername(new SecretString(projectConnection.getBitbucketUsername()))
                        .password(new SecretString(projectConnection.getPassword()))
                        .build();

        return WorkspaceFileV1.ProjectConnectionV1.builder()
                .id(projectConnection.getId())
                .type(projectConnection.getType())
                .version(BitbucketCloudProjectConnectionSettingsV1.VERSION)
                .settings(objectMapper.valueToTree(bitbucketCloudProjectConnectionSettingsV1)).build();
    }

    @Override
    public boolean supports(String type) {
        return type.equals(BitbucketCloudProjectConnection.TYPE);
    }
}

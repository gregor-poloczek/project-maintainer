package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnectionAdapter;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.UnsupportedProjectConnectionVersionException;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceFileV1;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class IntegrationTestFileSystemProjectConnectionAdapter implements ProjectConnectionAdapter<IntegrationTestFileSystemProjectConnection> {
    private static final String REMOTE_REPOSITORIES = "remote-repositories";

    @Override
    public boolean supports(String type) {
        return IntegrationTestFileSystemProjectConnection.TYPE.equals(type);
    }

    @Override
    public IntegrationTestFileSystemProjectConnection convert(ObjectMapper objectMapper, WorkspaceFileV1.ProjectConnectionV1 projectConnectionV1) throws UnsupportedProjectConnectionVersionException {
        IntegrationTestFileSystemProjectConnection.IntegrationTestFileSystemProjectConnectionBuilder builder =
                IntegrationTestFileSystemProjectConnection.builder()
                        .id(projectConnectionV1.getId());
        for (JsonNode remoteRepository : projectConnectionV1.getSettings().path(REMOTE_REPOSITORIES)) {
            builder.remoteRepository(Path.of(remoteRepository.asText()));
        }
        return builder.build();
    }

    @Override
    public WorkspaceFileV1.ProjectConnectionV1 convert(ObjectMapper objectMapper, IntegrationTestFileSystemProjectConnection projectConnection) {
        List<String> remoteRepositories = projectConnection.getRemoteRepositories().stream().map(Path::toString).toList();
        return WorkspaceFileV1.ProjectConnectionV1.builder()
                .id(projectConnection.getId())
                .type(projectConnection.getType())
                .version("1")
                .settings(objectMapper.valueToTree(Map.of(REMOTE_REPOSITORIES, remoteRepositories)))
                .build();
    }
}

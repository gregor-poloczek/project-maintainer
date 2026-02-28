package io.github.gregorpoloczek.projectmaintainer.patching.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnectionAdapter;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.UnsupportedProjectConnectionVersionException;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceFileV1;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IntegrationTestFileSystemProjectConnectionAdapter implements ProjectConnectionAdapter<IntegrationTestFileSystemProjectConnection> {
    @Override
    public boolean supports(String type) {
        return IntegrationTestFileSystemProjectConnection.TYPE.equals(type);
    }

    @Override
    public IntegrationTestFileSystemProjectConnection convert(ObjectMapper objectMapper, WorkspaceFileV1.ProjectConnectionV1 projectConnectionV1) throws UnsupportedProjectConnectionVersionException {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public WorkspaceFileV1.ProjectConnectionV1 convert(ObjectMapper objectMapper, IntegrationTestFileSystemProjectConnection projectConnection) {

        return WorkspaceFileV1.ProjectConnectionV1.builder()
                .id(projectConnection.getId())
                .type(projectConnection.getType())
                .version("1")
                .settings(objectMapper.valueToTree(Map.of()))
                .build();
    }
}

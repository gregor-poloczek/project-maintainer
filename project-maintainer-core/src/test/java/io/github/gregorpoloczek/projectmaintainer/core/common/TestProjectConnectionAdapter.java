package io.github.gregorpoloczek.projectmaintainer.core.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnectionAdapter;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceFileV1;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TestProjectConnectionAdapter implements ProjectConnectionAdapter<TestProjectConnection> {

    private static final String PROJECT_NAMES = "project-names";

    @Override
    public boolean supports(String type) {
        return TestProjectConnection.TYPE.equals(type);
    }

    @Override
    public TestProjectConnection convert(ObjectMapper objectMapper, WorkspaceFileV1.ProjectConnectionV1 projectConnectionV1) {
        TestProjectConnection.TestProjectConnectionBuilder builder = TestProjectConnection.builder()
                .id(projectConnectionV1.getId());
        for (JsonNode projectName : projectConnectionV1.getSettings().path(PROJECT_NAMES)) {
            builder.projectName(projectName.asText());
        }
        return builder.build();
    }

    @Override
    public WorkspaceFileV1.ProjectConnectionV1 convert(ObjectMapper objectMapper, TestProjectConnection projectConnection) {
        return WorkspaceFileV1.ProjectConnectionV1.builder()
                .id(projectConnection.getId())
                .type(projectConnection.getType())
                .version("1")
                .settings(objectMapper.valueToTree(Map.of(PROJECT_NAMES, projectConnection.getProjectNames())))
                .build();
    }
}

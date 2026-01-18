package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectImpl;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkspaceFileReader {

    @Qualifier("workspaceFileObjectMapper")
    ObjectMapper workspaceFileObjectMapper;
    List<ProjectConnectionAdapter<?>> projectConnectionAdapters;

    @Value
    public static class ReadResult {
        WorkspaceImpl workspace;
        List<ProjectImpl> projects;
    }

    public ReadResult convertYamlFileToWorkspace(Path workspaceFilePath) throws InvalidWorkspaceFileException {
        if (!Files.exists(workspaceFilePath)) {
            throw new InvalidWorkspaceFileException(workspaceFilePath, "No workspace file found at %s".formatted(workspaceFilePath.toString()));
        }
        try {
            String content = IOUtils.toString(workspaceFilePath.toUri(), StandardCharsets.UTF_8);
            Optional<String> maybeVersion = Optional.of(workspaceFileObjectMapper.readTree(content)).filter(JsonNode::isObject)
                    .map(ObjectNode.class::cast)
                    .map(o -> o.get("version"))
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .filter(StringUtils::isNotBlank);

            if (maybeVersion.isEmpty()) {
                throw new InvalidWorkspaceFileException(workspaceFilePath, "%s does not denote a valid yaml file with a version field".formatted(workspaceFilePath));
            }
            String version = maybeVersion.get();
            if (version.equals(WorkspaceFileV1.VERSION)) {
                WorkspaceFileV1.WorkspaceV1 workspaceV1 = workspaceFileObjectMapper
                        .readValue(content, WorkspaceFileV1.class)
                        .getWorkspace();

                List<Project> projects = workspaceV1.getProjects().stream().map(p -> this.fromYamlToProject(workspaceV1.id, p)).toList();

                WorkspaceImpl workspace = WorkspaceImpl.builder()
                        .directory(workspaceFilePath.getParent())
                        .name(workspaceV1.getName())
                        .id(workspaceV1.getId())
                        .projectConnections(workspaceV1.getProjectConnections().
                                stream()
                                .map(this::toConnection)
                                .toList())
                        .projects(projects)
                        .build();

                return new ReadResult(workspace, workspace.getProjects().stream().map(ProjectImpl.class::cast).toList());
            } else {
                throw new InvalidWorkspaceFileException(workspaceFilePath, "Version %s of workspace %s is not supported".formatted(version, workspaceFilePath));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ProjectConnection toConnection(WorkspaceFileV1.ProjectConnectionV1 v) {
        try {
            return this.projectConnectionAdapters.stream().filter(a -> a.supports(v.getType())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported connection type: " + v.getType()))
                    .convert(workspaceFileObjectMapper, v);
        } catch (UnsupportedProjectConnectionVersionException e) {
            throw new IllegalStateException(e);
        }
    }

    private Project fromYamlToProject(String workspaceId, WorkspaceFileV1.ProjectV1 p) {
        WorkspaceFileV1.ProjectMetaDataV1 metaDataV1 = p.getMetaData();
        ProjectMetaData metaData = ProjectMetaData.builder()
                .fqpn(p.getFQPN())
                .uri(metaDataV1.getUri())
                .name(metaDataV1.getName())
                .description(metaDataV1.getDescription())
                .owner(metaDataV1.getOwner())
                .websiteLink(metaDataV1.getWebsiteLink())
                .browserLink(metaDataV1.getBrowserLink())
                .build();
        return new ProjectImpl(workspaceId, p.getConnectionId(), metaData);
    }

}

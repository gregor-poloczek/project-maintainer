package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkspaceFileWriter {
    @Qualifier("workspaceFileObjectMapper")
    ObjectMapper workspaceFileObjectMapper;
    List<ProjectConnectionAdapter<?>> projectConnectionAdapters;

    public void write(WorkspaceImpl workspace, Path workspaceFile) throws IOException {
        WorkspaceFileV1.WorkspaceV1.WorkspaceV1Builder b = WorkspaceFileV1.WorkspaceV1.builder().name(workspace.getName()).id(workspace.getId());

        b.projectConnections(workspace.projectConnections.stream()
                .map(this::fromConnectionToYaml).toList());
        b.projects(workspace.projects.stream()
                .map(this::fromProjectToYaml).toList());
        WorkspaceFileV1 ymlContents = WorkspaceFileV1.builder()
                .version(WorkspaceFileV1.VERSION)
                .workspace(b.build())
                .build();

        try (FileOutputStream out = new FileOutputStream(workspaceFile.toFile(), false)) {
            workspaceFileObjectMapper.writeValue(out, ymlContents);
        } catch (IOException e) {
            Files.deleteIfExists(workspaceFile);
            throw e;
        }
    }

    private WorkspaceFileV1.ProjectV1 fromProjectToYaml(Project project) {
        ProjectMetaData metaData = project.getMetaData();
        return WorkspaceFileV1.ProjectV1.builder()
                .connectionId(project.getConnectionId())
                .fqpn(project.getFQPN())
                .metaData(WorkspaceFileV1.ProjectMetaDataV1.builder()
                        .name(metaData.getName())
                        .owner(metaData.getOwner())
                        .description(metaData.getDescription().orElse(null))
                        .uri(metaData.getURI())
                        .websiteLink(metaData.getWebsiteLink().orElse(null))
                        .browserLink(metaData.getBrowserLink().orElse(null))
                        .build())
                .build();
    }

    private WorkspaceFileV1.ProjectConnectionV1 fromConnectionToYaml(ProjectConnection projectConnection) {
        return this.projectConnectionAdapters.stream().filter(a -> a.supports(projectConnection)).findFirst()
                .map(a -> (ProjectConnectionAdapter<ProjectConnection>) a)
                .orElseThrow(() -> new IllegalStateException("Unexpected connection type: " + projectConnection.getClass().getName()))
                .convert(workspaceFileObjectMapper, projectConnection);
    }

}

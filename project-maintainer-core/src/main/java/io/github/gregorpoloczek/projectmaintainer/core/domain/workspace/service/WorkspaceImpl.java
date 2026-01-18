package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class WorkspaceImpl implements Workspace {
    @NonNull
    Path directory;
    @NonNull
    @ToString.Include
    String id;
    @NonNull
    @ToString.Include
    String name;
    @Builder.Default
    List<ProjectConnection> projectConnections = new ArrayList<>();
    @Builder.Default
    List<Project> projects = new ArrayList<>();
}

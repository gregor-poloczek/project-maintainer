package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import java.nio.file.Path;
import java.util.List;

public interface Workspace {
    String getId();

    String getName();

    Path getDirectory();

    List<ProjectConnection> getProjectConnections();
}

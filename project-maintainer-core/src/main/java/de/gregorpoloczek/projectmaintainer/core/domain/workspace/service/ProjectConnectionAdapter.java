package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ProjectConnectionAdapter<T extends ProjectConnection> {
    boolean supports(String type);

    default boolean supports(ProjectConnection projectConnection) {
        return supports(projectConnection.getType());
    }

    T convert(ObjectMapper objectMapper, WorkspaceFileV1.ProjectConnectionV1 projectConnectionV1) throws UnsupportedProjectConnectionVersionException;

    WorkspaceFileV1.ProjectConnectionV1 convert(ObjectMapper objectMapper, T projectConnection);
}

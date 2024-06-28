package de.gregorpoloczek.projectmaintainer.core.domain.communication.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

public interface ProjectOperationProgressListener {

    void scheduled();

    void send(ProjectOperationProgress progress);

    void succeeded(Project project);

    void failed(Project project, final Throwable e);

    void update(final String message, double percentage);

    void update(final String message);
}

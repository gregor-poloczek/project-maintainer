package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;

public interface ProjectOperationProgressListener {

  void scheduled();

  void send(ProjectOperationProgress progress);

  void succeeded(Project project);

  void failed(Project project, final Throwable e);

  void update(final String message, double progress);

  void update(final String message);
}

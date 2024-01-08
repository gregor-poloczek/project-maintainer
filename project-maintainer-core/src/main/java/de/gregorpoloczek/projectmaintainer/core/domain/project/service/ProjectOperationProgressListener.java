package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

public interface ProjectOperationProgressListener {

  void scheduled();

  void send(OperationProgress progress);

  void succeeded();

  void failed(final Throwable e);

  void update(final String message, double progress);
}

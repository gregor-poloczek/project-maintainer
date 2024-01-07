package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

public interface CloneListener {

  void update(CloneProgress cloneProgress);

  void complete();

  void fail(Throwable e);
}

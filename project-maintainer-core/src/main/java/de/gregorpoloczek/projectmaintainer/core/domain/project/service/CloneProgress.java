package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;

public interface CloneProgress {

  FQPN getFQPN();

  int getTotalTasks();

  int getTotalTasksDone();

  String getCurrentTaskTitle();

  int getCurrentTaskTotalWork();

  int getCurrentTaskTotalWorkDone();
}

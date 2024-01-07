package de.gregorpoloczek.projectmaintainer.core.git.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ProgressMonitor;

@Slf4j
public class GitCloneProgressMonitor implements ProgressMonitor {

  private final CloneListener cloneListener;
  private final CloneProgressImpl progress;


  @Getter
  @ToString
  private static class CloneProgressImpl implements CloneProgress {

    int totalTasks;
    int totalTasksDone;
    String currentTaskTitle;
    int currentTaskTotalWork;
    int currentTaskTotalWorkDone;
    private final FQPN fqpn;

    private CloneProgressImpl(final FQPN fqpn) {
      this.fqpn = fqpn;
    }

    @Override
    public FQPN getFQPN() {
      return this.fqpn;
    }
  }


  public GitCloneProgressMonitor(final FQPN fqpn, final CloneListener cloneListener) {
    this.cloneListener = cloneListener;
    this.progress = new CloneProgressImpl(fqpn);
  }


  @Override
  public void start(final int totalTasks) {
    progress.totalTasks = totalTasks;
    notifyListener();
  }

  @Override
  public void beginTask(final String title, final int totalWork) {
    progress.currentTaskTitle = title;
    progress.currentTaskTotalWork = totalWork;
    notifyListener();
  }

  @Override
  public void update(final int completed) {
    progress.currentTaskTotalWorkDone += completed;
    if (completed == 0 || progress.currentTaskTotalWork == 0) {
      return;
    }

    int tenPercent = (int) Math.ceil((double) progress.currentTaskTotalWork / 10.0d);
    if (progress.currentTaskTotalWork % tenPercent == 0) {
      this.notifyListener();
    }
  }

  @Override
  public void endTask() {
    progress.currentTaskTitle = null;
    progress.totalTasksDone++;
    notifyListener();
  }

  private void notifyListener() {
    GitCloneProgressMonitor.this.cloneListener.update(progress);
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public void showDuration(final boolean enabled) {

  }
}

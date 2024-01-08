package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ProgressMonitor;

@Slf4j
public class GitCloneProgressMonitor implements ProgressMonitor {

  private final ProjectOperationProgressListener listener;
  private final CloneProgressImpl progress;


  @Getter
  @ToString
  private static class CloneProgressImpl {

    int totalTasks;
    int totalTasksDone;
    String currentTaskTitle;
    int currentTaskTotalWork;
    int currentTaskTotalWorkDone;

    private CloneProgressImpl() {

    }

  }


  public GitCloneProgressMonitor(final ProjectOperationProgressListener listener) {
    this.listener = listener;
    this.progress = new CloneProgressImpl();
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
    progress.currentTaskTotalWorkDone = 0;
    notifyListener();
  }

  @Override
  public void update(final int completed) {
    progress.currentTaskTotalWorkDone += completed;
    if (completed == 0 || progress.currentTaskTotalWork == 0) {
      return;
    }

    int tenPercent = (int) Math.ceil((double) progress.currentTaskTotalWork / 10.0d);
    if (progress.currentTaskTotalWorkDone == progress.currentTaskTotalWork
        || progress.currentTaskTotalWorkDone % tenPercent == 0) {
      this.notifyListener();
    }

  }

  @Override
  public void endTask() {
    progress.currentTaskTitle = null;
    progress.currentTaskTotalWork = -1;
    progress.currentTaskTotalWorkDone = -1;
    progress.totalTasksDone++;
    notifyListener();
  }

  private void notifyListener() {
    double progress = -1d;
    if (this.progress.currentTaskTotalWork > 0) {
      progress = (double) this.progress.currentTaskTotalWorkDone
          / (double) this.progress.currentTaskTotalWork;
    }
    GitCloneProgressMonitor.this.listener.update(this.progress.currentTaskTitle, progress);
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public void showDuration(final boolean enabled) {

  }
}

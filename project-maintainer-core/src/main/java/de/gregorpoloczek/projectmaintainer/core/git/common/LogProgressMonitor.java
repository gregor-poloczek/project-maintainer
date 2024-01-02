package de.gregorpoloczek.projectmaintainer.core.git.common;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.springframework.stereotype.Component;

@Component
@Slf4j

public class LogProgressMonitor implements ProgressMonitor {

  @Override
  public void start(final int i) {

  }

  @Override
  public void beginTask(final String s, final int i) {
    log.info("{} {}", s, i);
  }

  @Override
  public void update(final int i) {

  }

  @Override
  public void endTask() {

  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public void showDuration(final boolean b) {

  }
}

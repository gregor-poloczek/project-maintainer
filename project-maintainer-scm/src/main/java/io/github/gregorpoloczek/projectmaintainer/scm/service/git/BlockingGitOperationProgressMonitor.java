package io.github.gregorpoloczek.projectmaintainer.scm.service.git;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgressListener;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Reports the progress of a JGit operation running in a blocking way to a {@link ProjectOperationProgressListener}.
 * <p>
 * The operation is cancelled as soon as the thread running it is interrupted.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class BlockingGitOperationProgressMonitor<T> implements ProgressMonitor {

    final ProjectOperationProgressListener<T> progressListener;
    final FQPN fqpn;

    String currentTaskTitle;
    int currentTaskTotalWork;
    int currentTaskTotalWorkDone;
    Instant lastNotification = null;

    @Override
    public void start(final int totalTasks) {
        notifyListener();
    }

    @Override
    public void beginTask(final String title, final int totalWork) {
        this.currentTaskTitle = title;
        this.currentTaskTotalWork = totalWork;
        this.currentTaskTotalWorkDone = 0;
        notifyListener();
    }

    @Override
    public void update(final int completed) {
        this.currentTaskTotalWorkDone += completed;
        if (completed == 0 || this.currentTaskTotalWork == 0) {
            return;
        }

        // notify on completion, every ten percent, or at the latest every 250ms
        final Instant now = Instant.now();
        int tenPercent = (int) Math.ceil((double) this.currentTaskTotalWork / 10.0d);
        boolean notify = this.currentTaskTotalWorkDone == this.currentTaskTotalWork
                || (tenPercent > 0 && this.currentTaskTotalWorkDone % tenPercent == 0);

        notify |= lastNotification != null
                && Duration.between(lastNotification, now).get(ChronoUnit.NANOS) >= 250 * 1000 * 1000;

        if (notify) {
            this.notifyListener();
        }
    }

    @Override
    public void endTask() {
        this.currentTaskTitle = null;
        this.currentTaskTotalWork = 1;
        this.currentTaskTotalWorkDone = 0;
        notifyListener();
    }

    private void notifyListener() {
        this.progressListener.onProgress(ProjectOperationProgress.<T>builder()
                .fqpn(this.fqpn)
                .state(OperationProgress.State.RUNNING)
                .message(this.currentTaskTitle)
                .progressCurrent(this.currentTaskTotalWorkDone)
                .progressTotal(this.currentTaskTotalWork)
                .build());
        this.lastNotification = Instant.now();
    }

    @Override
    public boolean isCancelled() {
        return Thread.currentThread().isInterrupted();
    }

    @Override
    public void showDuration(final boolean enabled) {
    }
}

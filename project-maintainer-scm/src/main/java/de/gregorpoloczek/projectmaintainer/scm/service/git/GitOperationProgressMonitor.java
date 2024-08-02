package de.gregorpoloczek.projectmaintainer.scm.service.git;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ProgressMonitor;
import reactor.core.publisher.FluxSink;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class GitOperationProgressMonitor<T> implements ProgressMonitor {

    final FluxSink<ProjectOperationProgress<T>> sink;
    final FQPN fqpn;

    String currentTaskTitle;
    int currentTaskTotalWork;
    int currentTaskTotalWorkDone;

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

    private Instant lastNotification = null;

    @Override
    public void update(final int completed) {
        // TODO seriously overthink this logic
        this.currentTaskTotalWorkDone += completed;
        if (completed == 0 || this.currentTaskTotalWork == 0) {
            return;
        }

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
        // TODO encode task title into progress
        this.sink.next(ProjectOperationProgress.<T>builder()
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
        return sink.isCancelled();
    }

    @Override
    public void showDuration(final boolean enabled) {

    }
}

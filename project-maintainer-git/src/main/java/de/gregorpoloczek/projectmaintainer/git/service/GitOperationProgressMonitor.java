package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ProgressMonitor;
import reactor.core.publisher.FluxSink;

@Slf4j
public class GitOperationProgressMonitor<T> implements ProgressMonitor {

    private final CloneProgressImpl progress;
    private final FluxSink<ProjectOperationProgress<T>> sink;
    private FQPN fqpn;


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


    public GitOperationProgressMonitor(final FluxSink<ProjectOperationProgress<T>> sink, final FQPN fqpn) {
        this.sink = sink;
        this.progress = new CloneProgressImpl();
        this.fqpn = fqpn;
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

    private Instant lastNotification = null;

    @Override
    public void update(final int completed) {
        // TODO seriously overthink this logic
        progress.currentTaskTotalWorkDone += completed;
        if (completed == 0 || progress.currentTaskTotalWork == 0) {
            return;
        }

        final Instant now = Instant.now();
        int tenPercent = (int) Math.ceil((double) progress.currentTaskTotalWork / 10.0d);
        boolean notify = progress.currentTaskTotalWorkDone == progress.currentTaskTotalWork
                || (tenPercent > 0 && progress.currentTaskTotalWorkDone % tenPercent == 0);

        notify |= lastNotification != null
                && Duration.between(lastNotification, now).get(ChronoUnit.NANOS) >= 250 * 1000 * 1000;

        if (notify) {
            this.notifyListener();
        }
    }

    @Override
    public void endTask() {
        progress.currentTaskTitle = null;
        progress.currentTaskTotalWork = 1;
        progress.currentTaskTotalWorkDone = 0;
        progress.totalTasksDone++;
        notifyListener();
    }

    private void notifyListener() {
        // TODO encode task title into progress
        this.sink.next(ProjectOperationProgress.<T>builder()
                .fqpn(this.fqpn)
                .state(ProjectOperationProgress.State.RUNNING)
                .message(this.progress.currentTaskTitle)
                .progressCurrent(this.progress.currentTaskTotalWorkDone)
                .progressTotal(this.progress.currentTaskTotalWork)
                .build());
        this.lastNotification = Instant.now();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void showDuration(final boolean enabled) {

    }
}

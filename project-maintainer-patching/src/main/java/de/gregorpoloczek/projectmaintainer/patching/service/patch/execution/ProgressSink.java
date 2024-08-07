package de.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress.ProjectOperationProgressBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;

@Slf4j
public class ProgressSink<T> {

    private final ProjectOperationProgressBuilder<T> builder;
    FluxSink<ProjectOperationProgress<T>> sink;

    public ProgressSink(FluxSink<ProjectOperationProgress<T>> sink,
            ProjectOperationProgressBuilder<T> builder) {
        this.sink = sink;
        this.builder = builder;
    }

    Integer totalProgress;
    Integer currentProgress;
    private String message;

    ProgressSink<T> total(int total) {
        this.currentProgress = 0;
        this.totalProgress = total;
        this.message = null;
        return this;
    }

    void done() {
        this.currentProgress++;
    }

    void next(String message) {
        this.message = message;
        this.currentProgress++;
        this.publish();
    }

    private void next() {
        this.message = null;
        this.currentProgress++;
        this.publish();
    }

    public void publish() {
        if (currentProgress > totalProgress) {
            log.warn("Progress has more effective steps than anticipated ones ({}/{}})",
                    currentProgress,
                    totalProgress);
        }
        log.info("{}/{} : {}", currentProgress, totalProgress, message);

        this.sink.next(builder.progressTotal(totalProgress)
                .progressCurrent(currentProgress)
                .message(this.message)
                .build());
    }
}

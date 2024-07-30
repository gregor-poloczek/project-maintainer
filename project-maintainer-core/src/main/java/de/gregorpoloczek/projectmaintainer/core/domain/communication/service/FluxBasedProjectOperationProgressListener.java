package de.gregorpoloczek.projectmaintainer.core.domain.communication.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;
import reactor.core.publisher.FluxSink;

@Deprecated
public class FluxBasedProjectOperationProgressListener implements
        ProjectOperationProgressListener {

    public static final double NO_PERCENTAGE = -1d;
    private final FQPN fqpn;
    private final String operation;
    private BiConsumer<FQPN, Optional<Throwable>> onComplete;
    private final FluxSink<ProjectOperationProgress> sink;


    public FluxBasedProjectOperationProgressListener(FluxSink<ProjectOperationProgress> sink,
            final FQPN fqpn,
            final String operation,
            BiConsumer<FQPN, Optional<Throwable>> onComplete) {
        this.sink = sink;
        this.fqpn = fqpn;
        this.operation = operation;
        this.onComplete = onComplete;
    }


    public void scheduled() {
        this.send(
                new ProjectOperationProgress(fqpn, operation)
                        .with(ProjectOperationState.SCHEDULED)
                        .withProgress(NO_PERCENTAGE));
    }

    public void send(ProjectOperationProgress progress) {
        sink.next(progress.withTimestamp(Instant.now()));
    }

    public void succeeded(Project project) {
        this.send(
                new ProjectOperationProgress(fqpn, operation)
                        .with(ProjectOperationState.SUCCEEDED)
                        .withProgress(NO_PERCENTAGE)

        )
        ;
        this.onComplete.accept(fqpn, Optional.empty());
    }

    public void failed(Project project, final Throwable e) {
        this.send(new ProjectOperationProgress(fqpn, operation)
                .with(ProjectOperationState.FAILED)
                .withProgress(NO_PERCENTAGE)
        );
        this.onComplete.accept(fqpn, Optional.of(e));
    }

    public void update(final String message, double percentage) {
        this.send(
                new ProjectOperationProgress(fqpn, operation)
                        .with(ProjectOperationState.RUNNING)
                        .withMessage(message)
                        .withProgress(percentage)
        );
    }

    @Override
    public void update(final String message) {
        this.update(message, NO_PERCENTAGE);
    }
}

package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

public class SinkBasedProjectOperationProgressListener implements
        ProjectOperationProgressListener {

    public static final double NO_PERCENTAGE = -1d;
    private final FQPN fqpn;
    private final String operation;
    private BiConsumer<FQPN, Optional<Throwable>> onComplete;
    private final Sinks.Many<ProjectOperationProgress> sink;


    public SinkBasedProjectOperationProgressListener(Sinks.Many<ProjectOperationProgress> sink,
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
        sink.emitNext(progress.withTimestamp(Instant.now()),
                (s, e) -> e == EmitResult.FAIL_NON_SERIALIZED);
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

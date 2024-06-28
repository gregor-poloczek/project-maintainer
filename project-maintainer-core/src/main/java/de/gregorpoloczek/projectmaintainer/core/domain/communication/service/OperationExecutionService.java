package de.gregorpoloczek.projectmaintainer.core.domain.communication.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.api.FluxBasedProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class OperationExecutionService {

    private final Executor executor;

    final Sinks.Many<ProjectOperationProgress> sink = Sinks
            .many()
            .multicast()
            .onBackpressureBuffer();


    public OperationExecutionService(Executor executor) {
        this.executor = executor;
    }

    public Flux<ProjectOperationProgress> getUpdateEvents() {
        return sink.asFlux();
    }

    public Flux<ProjectOperationProgress> executeAsyncOperation2(final Project project, final String operationName,
            final BiConsumer<FQPN, ProjectOperationProgressListener> operation) {

        return Flux.create(s -> {
            FQPN fqpn = project.getMetaData().getFQPN();
            final ProjectOperationProgressListener emitter =
                    new FluxBasedProjectOperationProgressListener(s, fqpn, operationName,
                            (e, e2) -> e2.ifPresentOrElse(s::error, s::complete));
            emitter.scheduled();

            this.executor.execute(
                    () -> {
                        try {
                            operation.accept(fqpn, emitter);
                            emitter.succeeded(project);
                        } catch (RuntimeException e) {
                            emitter.failed(project, e);
                        }
                    }
            );
        });
    }

}

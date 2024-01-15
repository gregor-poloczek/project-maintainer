package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.OperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseEmitterBasedProjectOperationProgressListener implements
    ProjectOperationProgressListener {

  private final FQPN fqpn;
  private final String operation;
  private BiConsumer<FQPN, Optional<Throwable>> onComplete;


  public SseEmitterBasedProjectOperationProgressListener(SseEmitter emitter, final FQPN fqpn,
      final String operation,
      BiConsumer<FQPN, Optional<Throwable>> onComplete) {
    this.emitter = emitter;
    this.fqpn = fqpn;
    this.operation = operation;
    this.onComplete = onComplete;
  }

  private final SseEmitter emitter;

  public void scheduled() {
    this.send(
        new OperationProgress(fqpn, operation).with(OperationState.SCHEDULED).withProgress(-1d));
  }

  public void send(OperationProgress progress) {
    try {
      emitter.send(
          SseEmitter.event()
              .id(UUID.randomUUID().toString())
              .name("message")
              .data(progress, MediaType.APPLICATION_JSON));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void succeeded() {
    this.send(
        new OperationProgress(fqpn, operation).with(OperationState.SUCCEEDED).withProgress(-1d));
    this.onComplete.accept(fqpn, Optional.empty());
  }

  public void failed(final Throwable e) {
    this.send(new OperationProgress(fqpn, operation).with(OperationState.FAILED).withProgress(-1d));
    this.onComplete.accept(fqpn, Optional.of(e));
  }

  public void update(final String message, double progress) {
    this.send(
        new OperationProgress(fqpn, operation)
            .with(OperationState.RUNNING)
            .withMessage(message)
            .withProgress(progress)
    );
  }
}

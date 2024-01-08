package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationProgress {

  private final FQPN fpqn;
  private OperationState state = OperationState.SCHEDULED;
  private final String operation;
  private String message = null;
  private double progress = 0.0d;

  public OperationProgress(final FQPN fqpn, final String operation) {
    this.fpqn = fqpn;
    this.operation = operation;
  }

  public OperationProgress with(final OperationState operationState) {
    // TODO remove copy
    OperationProgress other = new OperationProgress(this.fpqn, this.operation);
    other.state = operationState;
    return other;
  }

  public OperationProgress withMessage(final String message) {
    this.message = message;
    return this;
  }

  public OperationProgress withProgress(final double progress) {
    this.progress = progress;
    return this;
  }
}

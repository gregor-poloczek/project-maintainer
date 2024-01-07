package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationProgress {

  private final FQPN fpqn;
  private OperationState state = OperationState.SCHEDULED;
  private final String operation;

  public OperationProgress(final FQPN fqpn, final String operation) {
    this.fpqn = fqpn;
    this.operation = operation;
  }

  public OperationProgress with(final OperationState operationState) {
    OperationProgress other = new OperationProgress(this.fpqn, this.operation);
    other.state = operationState;
    return other;
  }
}

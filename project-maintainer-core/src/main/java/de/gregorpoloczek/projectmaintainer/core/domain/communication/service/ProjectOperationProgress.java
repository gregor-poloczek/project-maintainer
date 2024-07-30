package de.gregorpoloczek.projectmaintainer.core.domain.communication.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Deprecated
@Getter
@Setter
public class ProjectOperationProgress {

    private final FQPN fqpn;
    private ProjectOperationState state = ProjectOperationState.SCHEDULED;
    private final String operation;
    private String message = null;
    private double progress = 0.0d;
    private Instant timestamp;

    public ProjectOperationProgress(final FQPN fqpn, final String operation) {
        this.fqpn = fqpn;
        this.operation = operation;
    }

    public ProjectOperationProgress with(final ProjectOperationState projectOperationState) {
        // TODO remove copy
        ProjectOperationProgress other = new ProjectOperationProgress(this.fqpn, this.operation);
        other.state = projectOperationState;
        return other;
    }

    public ProjectOperationProgress withMessage(final String message) {
        this.message = message;
        return this;
    }

    // TODO rename to percentage
    public ProjectOperationProgress withProgress(final double progress) {
        this.progress = progress;
        return this;
    }

    public ProjectOperationProgress withTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }


}

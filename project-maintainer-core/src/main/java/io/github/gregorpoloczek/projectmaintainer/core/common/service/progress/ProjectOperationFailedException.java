package io.github.gregorpoloczek.projectmaintainer.core.common.service.progress;

import lombok.Getter;

/**
 * Thrown by a blocking operation reporting its progress, after its failure has already been reported as
 * {@link OperationProgress.State#FAILED} progress.
 * <p>
 * Wraps the actual cause of the failure. Callers can rely on the failure being reported already, and hence must not
 * report it again.
 */
public class ProjectOperationFailedException extends RuntimeException {

    @Getter
    private final ProjectOperationProgress<?> progress;

    public ProjectOperationFailedException(ProjectOperationProgress<?> progress, Throwable cause) {
        super(cause.getMessage(), cause);
        this.progress = progress;
    }
}

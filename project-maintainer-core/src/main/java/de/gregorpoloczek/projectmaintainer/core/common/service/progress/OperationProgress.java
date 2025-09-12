package de.gregorpoloczek.projectmaintainer.core.common.service.progress;

import java.util.Optional;

public interface OperationProgress<T> {

    enum State {
        SCHEDULED, STARTED, RUNNING, DONE,
        // TODO do i need this?
        FAILED;

        public boolean isTerminated() {
            return this == State.DONE || this == State.FAILED;
        }
    }

    State getState();

    String getMessage();

    Optional<T> getResult();

    Optional<Throwable> getThrowable();

    int getProgressCurrent();

    int getProgressTotal();

    default double getProgressRelative() {
        return (double) getProgressCurrent() / (double) getProgressTotal();
    }

}

package de.gregorpoloczek.projectmaintainer.core.common.service.progress;

public interface OperationProgress<T> {

    enum State {
        SCHEDULED, RUNNING, DONE, FAILED;

        public boolean isTerminated() {
            return this == State.DONE || this == State.FAILED;
        }
    }

    State getState();

    String getMessage();

    T getResult();

    int getProgressCurrent();

    int getProgressTotal();

    default double getProgressRelative() {
        return (double) getProgressCurrent() / (double) getProgressTotal();
    }

}

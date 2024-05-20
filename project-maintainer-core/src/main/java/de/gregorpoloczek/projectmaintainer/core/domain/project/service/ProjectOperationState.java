package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

public enum ProjectOperationState {
    SCHEDULED(false), STARTED(false), RUNNING(false), SUCCEEDED(true), FAILED(true);

    ProjectOperationState(boolean terminated) {
        this.terminated = terminated;
    }

    private final boolean terminated;

    public boolean isTerminated() {
        return terminated;
    }
}

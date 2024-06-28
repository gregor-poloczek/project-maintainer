package de.gregorpoloczek.projectmaintainer.git.service;

public class ProjectPullFailedException extends RuntimeException {

    public ProjectPullFailedException(final Throwable cause) {
        super(cause);
    }
}

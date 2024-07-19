package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(final ProjectRelatable projectRelatable) {
        super("Could not find project \"%s\"".formatted(projectRelatable.getFQPN()));
    }
}

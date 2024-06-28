package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(final FQPN fqpn) {
        super("Could not find project \"%s\"".formatted(fqpn));
    }
}

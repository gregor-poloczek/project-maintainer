package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

public class UnsupportedProjectConnectionVersionException extends Exception {
    public UnsupportedProjectConnectionVersionException(String type, String version) {
        super("Version %s is not supported for type %s".formatted(version, type));
    }
}

package de.gregorpoloczek.projectmaintainer.core.domain.project.service;


import java.nio.file.Path;

public interface ProjectFileLocation extends ProjectRelatable, Comparable<ProjectFileLocation> {

    String getFileName();

    Path getRelativePath();

    // TODO hier rauswerfen?
    Path getAbsolutePath();

    @Override
    default int compareTo(ProjectFileLocation o) {
        return getRelativePath().compareTo(o.getRelativePath());
    }

    default boolean exists() {
        return getAbsolutePath().toFile().exists();
    }
}

package de.gregorpoloczek.projectmaintainer.core.domain.project.service;


import java.nio.file.Path;

public interface ProjectFileLocation extends ProjectRelatable {

    String getFileName();

    Path getRelativePath();

    // TODO hier rauswerfen?
    Path getAbsolutePath();
}

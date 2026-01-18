package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

public interface ProjectFiles {

    boolean hasAny(String regex);

    @Deprecated
    SortedSet<File> find(String regex);

    List<ProjectFileLocation> findLocations(String regex);

    Optional<ProjectFileLocation> findLocation(String regex);

    ProjectFileLocation get(String name);
}

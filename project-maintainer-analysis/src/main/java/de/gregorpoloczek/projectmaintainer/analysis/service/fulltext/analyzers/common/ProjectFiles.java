package de.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.io.File;
import java.util.List;
import java.util.SortedSet;

public interface ProjectFiles {

    boolean hasAny(String regex);

    @Deprecated
    SortedSet<File> find(String regex);

    List<ProjectFileLocation> findLocations(String regex);
}

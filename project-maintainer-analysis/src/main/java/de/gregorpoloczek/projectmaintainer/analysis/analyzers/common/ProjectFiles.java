package de.gregorpoloczek.projectmaintainer.analysis.analyzers.common;

import java.io.File;
import java.util.SortedSet;

public interface ProjectFiles {

    boolean hasAny(String regex);

    SortedSet<File> find(String regex);

}

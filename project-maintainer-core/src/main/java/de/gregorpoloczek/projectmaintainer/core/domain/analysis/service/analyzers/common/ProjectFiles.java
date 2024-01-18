package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common;

import java.io.File;
import java.util.SortedSet;

public interface ProjectFiles {

  boolean hasAny(String regex);

  SortedSet<File> find(String regex);

}

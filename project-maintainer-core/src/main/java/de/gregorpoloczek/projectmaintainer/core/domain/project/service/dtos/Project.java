package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
import java.io.File;
import java.util.SortedSet;
import java.util.function.Supplier;

public interface Project {

  boolean isCloned();

  File getDirectory();

  FQPN getFQPN();

  ProjectMetaData getMetaData();

  Commit getLatestCommit();

  <T> T withReadLock(Supplier<T> operation);

  <T> T withWriteLock(Supplier<T> operation);

  FactsCollector facts();

  SortedSet<Label> getLabels();
}

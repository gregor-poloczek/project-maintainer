package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

public interface WorkingCopy {

  File getDirectory();

  Optional<Commit> getLatestCommit();

  FQPN getFQPN();

  URI getURI();

  <T> T withReadLock(Supplier<T> operation);

  <T> T withWriteLock(Supplier<T> operation);
}

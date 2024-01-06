package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.git.common.Commit;
import java.io.File;
import java.net.URI;

public interface GitClonable {

  URI getURI();

  File getDirectory();

  void markAsCloned();

  void setLatestCommit(Commit commit);
}

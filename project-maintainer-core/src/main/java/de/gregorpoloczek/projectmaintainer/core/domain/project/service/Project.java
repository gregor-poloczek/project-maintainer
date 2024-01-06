package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.git.common.Commit;
import java.io.File;
import java.net.URI;

public interface Project {

  boolean isCloned();

  File getDirectory();

  URI getURI();

  FQPN getFQPN();

  Commit getLatestCommit();
}

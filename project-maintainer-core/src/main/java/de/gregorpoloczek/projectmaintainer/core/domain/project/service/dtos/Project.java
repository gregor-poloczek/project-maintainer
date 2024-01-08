package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;
import java.net.URI;

public interface Project {

  boolean isCloned();

  File getDirectory();

  URI getURI();

  FQPN getFQPN();

  Commit getLatestCommit();
}

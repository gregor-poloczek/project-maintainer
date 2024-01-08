package de.gregorpoloczek.projectmaintainer.core.domain.git;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;
import java.net.URI;

public interface GitClonable {

  URI getURI();

  File getDirectory();

  void markAsCloned();

  void setLatestCommit(Commit commit);

  FQPN getFQPN();
}

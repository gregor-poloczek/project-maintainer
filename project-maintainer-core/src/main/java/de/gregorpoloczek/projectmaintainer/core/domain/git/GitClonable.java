package de.gregorpoloczek.projectmaintainer.core.domain.git;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.io.File;
import java.net.URI;

public interface GitClonable extends Project {

  URI getURI();

  File getDirectory();

  void markAsCloned();

  void setLatestCommit(Commit commit);

  FQPN getFQPN();

  void markAsNotCloned();
}

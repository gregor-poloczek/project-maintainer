package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.ProjectMetaData;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;

public interface Project {

  boolean isCloned();

  File getDirectory();

  FQPN getFQPN();

  ProjectMetaData getMetaData();

  Commit getLatestCommit();
}

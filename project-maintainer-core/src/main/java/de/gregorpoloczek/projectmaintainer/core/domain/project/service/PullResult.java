package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.git.common.Commit;

public interface PullResult {

  Commit getLatestCommit();
}

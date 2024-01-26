package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import java.util.Optional;

public interface PullResult {

  Optional<Commit> getLatestCommit();
}

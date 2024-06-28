package de.gregorpoloczek.projectmaintainer.git.service;

import java.util.Optional;

public interface PullResult {

    Optional<Commit> getLatestCommit();
}

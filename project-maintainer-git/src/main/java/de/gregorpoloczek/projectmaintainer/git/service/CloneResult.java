package de.gregorpoloczek.projectmaintainer.git.service;

import java.util.Optional;

public interface CloneResult {

    Optional<Commit> getLatestCommit();
}

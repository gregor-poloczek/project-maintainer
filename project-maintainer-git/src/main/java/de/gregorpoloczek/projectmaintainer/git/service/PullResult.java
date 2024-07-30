package de.gregorpoloczek.projectmaintainer.git.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PullResult {

    private final Commit latestCommit;

    public Optional<Commit> getLatestCommit() {
        return Optional.of(latestCommit);
    }
}

package io.github.gregorpoloczek.projectmaintainer.scm.service.git;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PullResult {

    private final Commit latestCommit;

    public Optional<Commit> getLatestCommit() {
        return Optional.of(latestCommit);
    }
}

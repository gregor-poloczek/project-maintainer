package de.gregorpoloczek.projectmaintainer.git.service;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class CloneResult {

    private final Commit latestCommit;
    private final String currentBranch;

    Optional<Commit> getLatestCommit() {
        return Optional.ofNullable(latestCommit);
    }
}

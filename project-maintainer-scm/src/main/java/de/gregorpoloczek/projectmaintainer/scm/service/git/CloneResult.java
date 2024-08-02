package de.gregorpoloczek.projectmaintainer.scm.service.git;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class CloneResult {

    private final Commit latestCommit;
    private final String currentBranch;

    public Optional<Commit> getLatestCommit() {
        return Optional.ofNullable(latestCommit);
    }
}

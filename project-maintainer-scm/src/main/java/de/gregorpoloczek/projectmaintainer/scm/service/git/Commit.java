package de.gregorpoloczek.projectmaintainer.scm.service.git;


import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.eclipse.jgit.revwalk.RevCommit;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Commit {

    String hash;
    String authorName;
    String message;
    Instant timestamp;

    public static Commit of(final RevCommit latestCommit) {
        return new Commit(
                latestCommit.getId().abbreviate(7).name(),
                latestCommit.getAuthorIdent().getName(),
                latestCommit.getFullMessage(),
                Instant.ofEpochSecond(latestCommit.getCommitTime()));
    }
}

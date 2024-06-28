package de.gregorpoloczek.projectmaintainer.git.service;


import java.time.Instant;
import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

@Getter
public class CommitImpl implements Commit {

    private final String authorName;

    public CommitImpl(final String hash, String author, final String message, final Instant timestamp) {
        this.timestamp = timestamp;
        this.hash = hash;
        this.authorName = author;
        this.message = message;
    }

    private Instant timestamp;
    private String hash;
    private String message;

    public static CommitImpl of(final RevCommit latestCommit) {
        return new CommitImpl(
                latestCommit.getId().abbreviate(7).name(),
                latestCommit.getAuthorIdent().getName(),
                latestCommit.getFullMessage(),
                Instant.ofEpochSecond(latestCommit.getCommitTime()));
    }
}

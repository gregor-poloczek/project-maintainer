package de.gregorpoloczek.projectmaintainer.core.domain.git.service;


import java.time.Instant;
import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

@Getter
public class CommitImpl implements Commit {

  public CommitImpl(final String hash, final String message, final Instant timestamp) {
    this.timestamp = timestamp;
    this.hash = hash;
    this.message = message;
  }

  private Instant timestamp;
  private String hash;
  private String message;

  public static CommitImpl of(final RevCommit latestCommit) {
    return new CommitImpl(latestCommit.getName(), latestCommit.getFullMessage(),
        Instant.ofEpochSecond(latestCommit.getCommitTime()));
  }
}

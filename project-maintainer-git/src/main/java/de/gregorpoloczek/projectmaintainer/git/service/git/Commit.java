package de.gregorpoloczek.projectmaintainer.git.service.git;


import java.time.Instant;

public interface Commit {

    Instant getTimestamp();

    String getMessage();

    String getHash();

    String getAuthorName();
}

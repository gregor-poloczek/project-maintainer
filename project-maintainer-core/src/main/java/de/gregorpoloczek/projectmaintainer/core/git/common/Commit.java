package de.gregorpoloczek.projectmaintainer.core.git.common;


import java.time.Instant;

public interface Commit {

  Instant getTimestamp();

  String getMessage();

  String getHash();
}

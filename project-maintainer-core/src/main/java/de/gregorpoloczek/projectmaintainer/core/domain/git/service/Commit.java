package de.gregorpoloczek.projectmaintainer.core.domain.git.service;


import java.time.Instant;

public interface Commit {

  Instant getTimestamp();

  String getMessage();

  String getHash();
}

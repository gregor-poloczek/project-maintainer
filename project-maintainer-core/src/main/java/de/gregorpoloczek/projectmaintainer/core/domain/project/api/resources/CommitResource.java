package de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;

public record CommitResource(String hash, Instant timestamp, String message) {

  public static CommitResource of(final Commit latestCommit) {
    return new CommitResource(
        latestCommit.getHash(),
        latestCommit.getTimestamp(),
        StringUtils.trim(latestCommit.getMessage()));
  }
}

package de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;

public record ProjectResource(FQPN fqpn, GitResource git) {

  public static ProjectResource of(Project project) {

    WorkingCopyResource wcr =
        project.isCloned() ? new WorkingCopyResource(CommitResource.of(project.getLatestCommit()))
            : null;

    final GitResource git = new GitResource(project.getURI(), wcr);
    return new ProjectResource(project.getFQPN(),
        git);
  }
}

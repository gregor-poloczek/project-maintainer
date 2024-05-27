package de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;

public record ProjectResource(FQPN fqpn, ProjectMetaDataResource metaData, GitResource git) {

    public static ProjectResource of(Project project) {

        WorkingCopyResource wcr =
                project.isCloned() ? new WorkingCopyResource(
                        project.getLatestCommit() != null ?
                                CommitResource.of(project.getLatestCommit()) : null)
                        : null;

        final GitProvider provider;
        FQPN fqpn2 = project.getMetaData().getFQPN();
        if (fqpn2.getValue().startsWith("github")) {
            provider = GitProvider.GITHUB;
        } else if (fqpn2.getValue().startsWith("aws-codecommit")) {
            provider = GitProvider.AWS_CODECOMMIT;
        } else {
            provider = GitProvider.UNKNOWN;
        }

        final GitResource git = new GitResource(project.getMetaData().getURI(),
                provider, wcr);
        return new ProjectResource(
                fqpn2,
                ProjectMetaDataResource.of(project),
                git);
    }
}

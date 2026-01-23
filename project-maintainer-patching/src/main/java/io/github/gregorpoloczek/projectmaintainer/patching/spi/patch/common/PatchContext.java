package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFiles;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;

import java.util.function.Function;

public interface PatchContext extends ProjectRelatable {

    @Override
    FQPN getFQPN();

    void create(ProjectFileLocation location, String content);

    void update(ProjectFileLocation location, String content);

    void update(ProjectFileLocation location, Function<String, String> transformation);

    void delete(ProjectFileLocation location);

    void pullRequestTitle(String title);

    void pullRequestCommitMessage(String commitMessage);

    ProjectFiles files();

    PatchParameterArguments arguments();
}

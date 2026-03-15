package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFiles;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.operations.PatchOperations;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;

import java.util.function.UnaryOperator;

public interface PatchContext extends ProjectRelatable {

    @Override
    FQPN getFQPN();

    /**
     * @deprecated use {@link PatchOperations#create(ProjectFileLocation, String)}
     */
    @Deprecated
    default void create(ProjectFileLocation location, String content) {
        this.operations().create(location, content);
    }

    /**
     * @deprecated use {@link PatchOperations#update(ProjectFileLocation, String)}
     */
    @Deprecated
    default void update(ProjectFileLocation location, String content) {
        this.operations().update(location, content);
    }

    /**
     * @deprecated use {@link PatchOperations#update(ProjectFileLocation, UnaryOperator)}
     */
    @Deprecated
    default void update(ProjectFileLocation location, UnaryOperator<String> transformation) {
        this.operations().update(location, transformation);
    }

    /**
     * @deprecated use {@link PatchOperations#delete(ProjectFileLocation)}
     */
    @Deprecated
    default void delete(ProjectFileLocation location) {
        this.operations().delete(location);
    }

    void pullRequestTitle(String title);

    void pullRequestCommitMessage(String commitMessage);

    ProjectFiles files();

    /**
     * Returns the {@link PatchOperations}
     *
     * @return the {@link PatchOperations}
     */
    PatchOperations operations();


    /**
     * Returns the {@link PatchParameterArguments}
     *
     * @return the {@link PatchParameterArguments}
     */
    PatchParameterArguments arguments();
}

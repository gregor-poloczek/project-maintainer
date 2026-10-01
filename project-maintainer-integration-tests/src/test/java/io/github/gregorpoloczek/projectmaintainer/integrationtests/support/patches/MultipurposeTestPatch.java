package io.github.gregorpoloczek.projectmaintainer.integrationtests.support.patches;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.Patch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MultipurposeTestPatch implements Patch {
    public static final String ID = MultipurposeTestPatch.class.getSimpleName();

    public static class Parameters {
        public static final String ADD_FILENAME = "add-filename";
        public static final String EDIT_FILENAME = "edit-filename";
        public static final String DELETE_FILENAME = "delete-filename";
        public static final String PULL_REQUEST_TITLE = "pull-request-title";
        public static final String COMMIT_MESSAGE = "commit-message";
    }

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id(ID)
                .patchParameters(List.of(
                        PatchParameter.builder()
                                .id(Parameters.ADD_FILENAME)
                                .required(false)
                                .type(PatchParameterType.STRING)
                                .build(),
                        PatchParameter.builder()
                                .id(Parameters.EDIT_FILENAME)
                                .required(false)
                                .type(PatchParameterType.STRING)
                                .build(),
                        PatchParameter.builder()
                                .id(Parameters.DELETE_FILENAME)
                                .required(false)
                                .type(PatchParameterType.STRING)
                                .build(),
                        PatchParameter.builder()
                                .id(Parameters.PULL_REQUEST_TITLE)
                                .required(false)
                                .type(PatchParameterType.STRING)
                                .build(),
                        PatchParameter.builder()
                                .id(Parameters.COMMIT_MESSAGE)
                                .required(false)
                                .type(PatchParameterType.STRING)
                                .build()))
                .description(ID)
                .build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        patchingContext.arguments()
                .getString(Parameters.ADD_FILENAME).getValue()
                .map(filename -> patchingContext.files().get(filename))
                .ifPresent(l -> patchingContext.create(l, "My-Content"));

        patchingContext.arguments()
                .getString(Parameters.EDIT_FILENAME).getValue()
                .map(filename -> patchingContext.files().get(filename))
                .ifPresent(l -> patchingContext.update(l, "My-Content"));

        patchingContext.arguments()
                .getString(Parameters.DELETE_FILENAME).getValue()
                .map(filename -> patchingContext.files().get(filename))
                .ifPresent(patchingContext::delete);

        patchingContext.arguments()
                .getString(Parameters.PULL_REQUEST_TITLE).getValue()
                .ifPresent(patchingContext::pullRequestTitle);

        patchingContext.arguments()
                .getString(Parameters.COMMIT_MESSAGE).getValue()
                .ifPresent(patchingContext::pullRequestCommitMessage);

        log.info("Executing {}", this.getMetaData().getId());
    }
}

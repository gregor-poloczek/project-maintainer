package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WellKnownPatchParameters {
    public static final PatchParameter BRANCH = PatchParameter.builder()
            .id("project-maintainer::patching::branch")
            .name("Branch")
            .description("Patch will be applied in this branch. The branch will be created based on the default branch of the repository. The stoping of an currently open patch application (i.e. closing the pull request) needs the branch value to be defined identically.")
            .type(PatchParameterType.STRING)
            .required(false)
            .build();

}

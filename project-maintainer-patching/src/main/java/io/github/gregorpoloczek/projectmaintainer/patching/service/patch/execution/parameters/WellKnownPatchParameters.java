package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WellKnownPatchParameters {
    public static final PatchParameter BRANCH = PatchParameter.builder()
            .id("project-maintainer::patching::branch")
            .name("Branch")
            .description("Patch will be applied in this branch. The branch will be created based on the default branch of the repository.")
            .type(PatchParameterType.STRING)
            .required(false)
            .build();

}

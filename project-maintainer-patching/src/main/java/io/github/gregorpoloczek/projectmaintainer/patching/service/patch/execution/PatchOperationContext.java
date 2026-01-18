package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

public interface PatchOperationContext extends ProjectRelatable {

    String getBaseBranch();

    String getPatchBranch();
}

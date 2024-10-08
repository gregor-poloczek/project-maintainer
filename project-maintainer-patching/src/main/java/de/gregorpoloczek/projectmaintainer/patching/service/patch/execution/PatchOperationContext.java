package de.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

public interface PatchOperationContext extends ProjectRelatable {

    String getBaseBranch();

    String getPatchBranch();
}

package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.Patch;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Builder
@Getter
class PatchStopContext implements PatchOperationContext, ProjectRelatable {

    @NonNull
    WorkingCopy workingCopy;
    @NonNull
    Patch patch;
    @NonNull
    String defaultBranch;
    @NonNull
    String patchBranch;
    @NonNull
    String baseBranch;
    @NonNull
    ProgressSink<PatchStopResult> progressSink;

    @Override
    public FQPN getFQPN() {
        return this.workingCopy.getFQPN();
    }

    public void publish(String message) {
        this.progressSink.next(message);
    }
}

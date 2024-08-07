package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResult;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResultDetail;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchStopResult;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasProject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ProjectPatchItem
        extends AbstractComposable<ProjectPatchItem>
        implements ProjectRelatable, Comparable<ProjectPatchItem> {

    PatchExecutionResult patchExecutionResult;
    PatchStopResult patchStopResult;

    public String getState() {
        return
                getPatchExecutionResult()
                        .map(PatchOperationResult.class::cast)
                        .or(this::getPatchStopResult)
                        .map(PatchOperationResult::getDetail)
                        .map(PatchOperationResultDetail::getName).orElse("");
    }

    public Optional<PatchExecutionResult> getPatchExecutionResult() {
        return Optional.ofNullable(patchExecutionResult);
    }

    public Optional<PatchStopResult> getPatchStopResult() {
        return Optional.ofNullable(patchStopResult);
    }

    public Project getProject() {
        return this.requireComponent(HasProject.class).getProject();
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.getProject().getFQPN();
    }

    public void clearResult() {
        this.patchExecutionResult = null;
        this.patchStopResult = null;
    }

    @Override
    public int compareTo(ProjectPatchItem that) {
        return this.getFQPN().compareTo(that.getFQPN());
    }
}

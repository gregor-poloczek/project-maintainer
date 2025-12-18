package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResult;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;

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
        extends AbstractComposable<FQPN, ProjectPatchItem>
        implements ProjectRelatable, Comparable<ProjectPatchItem> {

    public Optional<PatchOperationResult> getPatchOperationResult() {
        return this.requireTrait(HasOperationProgress.class)
                .getOperationProgress()
                .flatMap(OperationProgress::getResult)
                .map(PatchOperationResult.class::cast);
    }

    public Project getProject() {
        return this.requireTrait(HasProject.class).getProject();
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.getProject().getFQPN();
    }

    @Override
    public FQPN getKey() {
        return this.getProject().getFQPN();
    }

    public void clearResult() {
        this.replaceTrait(HasOperationProgress.class, x -> HasOperationProgress.empty());
    }

    @Override
    public int compareTo(ProjectPatchItem that) {
        return this.getFQPN().compareTo(that.getFQPN());
    }
}

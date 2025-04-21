package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResult;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
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

    PatchOperationResult patchOperationResult;

    public Optional<PatchOperationResult> getPatchOperationResult() {
        return Optional.ofNullable(patchOperationResult);
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
        this.patchOperationResult = null;
    }

    @Override
    public int compareTo(ProjectPatchItem that) {
        return this.getFQPN().compareTo(that.getFQPN());
    }
}

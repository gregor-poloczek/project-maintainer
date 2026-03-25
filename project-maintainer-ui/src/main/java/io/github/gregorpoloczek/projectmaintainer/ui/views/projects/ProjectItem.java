package io.github.gregorpoloczek.projectmaintainer.ui.views.projects;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
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
public class ProjectItem
        extends AbstractComposable<FQPN, ProjectItem>
        implements ProjectRelatable, Comparable<ProjectItem> {

    private Project getProject() {
        return this.requireTrait(HasProject.class).getProject();
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.getProject().getFQPN();
    }

    @Override
    public FQPN getKey() {
        return this.getFQPN().getFQPN();
    }

    @Override
    public int compareTo(ProjectItem that) {
        return this.getFQPN().compareTo(that.getFQPN());
    }
}

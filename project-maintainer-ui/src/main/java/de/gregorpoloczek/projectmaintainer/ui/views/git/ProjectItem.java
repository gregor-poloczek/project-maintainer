package de.gregorpoloczek.projectmaintainer.ui.views.git;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
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
        extends AbstractComposable<ProjectItem>
        implements ProjectRelatable, Comparable<ProjectItem> {

    private de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project getProject() {
        return this.requireTrait(HasProject.class).getProject();
    }

    public boolean matches(String query) {
        return getProject().getFQPN().toString().toLowerCase().contains(query.toLowerCase());
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.getProject().getFQPN();
    }

    @Override
    public int compareTo(ProjectItem that) {
        return this.getFQPN().compareTo(that.getFQPN());
    }
}

package de.gregorpoloczek.projectmaintainer.ui.views.git;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.HasProject;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasWorkingCopy;
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
public class ProjectItem
        extends AbstractComposable<ProjectItem>
        implements ProjectRelatable, HasWorkingCopy {

    WorkingCopy workingCopy;

    private de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project getProject() {
        return this.requireComponent(HasProject.class).getProject();
    }

    // TODO diese ganzen methoden als eine art "traits" verpacken, die ohne interfaces aus kommen
    public Optional<WorkingCopy> getWorkingCopy() {
        return Optional.ofNullable(workingCopy);
    }

    String text = "";
    String description = "";
    String website = "";
    String owner;

    public boolean matches(String query) {
        return getProject().getFQPN().toString().toLowerCase().contains(query.toLowerCase());
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.getProject().getFQPN();
    }
}

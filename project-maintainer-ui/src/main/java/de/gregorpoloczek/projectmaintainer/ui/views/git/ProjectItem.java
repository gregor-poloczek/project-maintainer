package de.gregorpoloczek.projectmaintainer.ui.views.git;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.git.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasIconItem;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasProjectItem;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectItem implements HasProjectItem, HasIconItem, ProjectRelatable, HasWorkingCopy {

    OperationProgress.State operationState = null;
    Project project;
    WorkingCopy workingCopy;
    Image icon;

    // TODO diese ganzen methoden als eine art "traits" verpacken, die ohne interfaces aus kommen
    public Optional<WorkingCopy> getWorkingCopy() {
        return Optional.ofNullable(workingCopy);
    }

    public Optional<Image> getIcon() {
        return Optional.ofNullable(icon);
    }

    String text = "";
    String description = "";
    String website = "";
    String owner;
    boolean operationInProgress;
    Double operationProgressValue;

    @Override
    public boolean isIconBlurred() {
        return this.workingCopy == null;
    }

    public boolean matches(String query) {
        return project.getFQPN().toString().toLowerCase().contains(query.toLowerCase());
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.project.getFQPN();
    }
}

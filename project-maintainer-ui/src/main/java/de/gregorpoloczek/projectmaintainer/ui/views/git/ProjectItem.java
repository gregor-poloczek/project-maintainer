package de.gregorpoloczek.projectmaintainer.ui.views.git;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasIconItem;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasProjectItem;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectItem implements HasProjectItem, HasIconItem, ProjectRelatable {

    OperationProgress.State operationState = null;
    Project project;
    Optional<WorkingCopy> workingCopy;
    Optional<Image> icon;
    String text = "";
    String description = "";
    String website = "";
    String owner;
    boolean operationInProgress;
    Double operationProgressValue;

    @Override
    public boolean isIconBlurred() {
        return this.workingCopy.isEmpty();
    }

    public boolean matches(String query) {
        return project.getFQPN().toString().toLowerCase().contains(query.toLowerCase());
    }

    @Override
    public FQPN getFQPN() {
        return this.project.getFQPN();
    }
}

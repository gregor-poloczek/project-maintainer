package de.gregorpoloczek.projectmaintainer.core.common.ui.git;

import com.vaadin.flow.component.grid.Grid.Column;
import de.gregorpoloczek.projectmaintainer.core.common.ui.shared.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProjectItem {

    private ProjectOperationState operationState = null;
    private Project project;
    private Optional<Image> image;
    private String text = "";
    private String owner;
    private boolean operationInProgress;
    private Double operationProgressValue;

    public String getName() {
        return this.getOwner() + " / " + this.getProject().getMetaData().getName();
    }
}

package de.gregorpoloczek.projectmaintainer.core.common.ui.git;

import de.gregorpoloczek.projectmaintainer.core.common.ui.shared.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private Optional<Commit> latestCommit;
    private String text = "";
    private String owner;
    private boolean operationInProgress;
    private Double operationProgressValue;

    public String getNamePrefix() {
        return this.project.getFQPN().getSegments()
                .stream()
                .skip(1)
                .filter(s -> !s.equals(this.project.getMetaData().getName()))
                .collect(Collectors.joining(" / "));
    }

    public String getName() {
        return this.getProject().getMetaData().getName();
    }
}

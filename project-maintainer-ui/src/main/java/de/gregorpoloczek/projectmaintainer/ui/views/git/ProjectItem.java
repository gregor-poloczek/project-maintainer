package de.gregorpoloczek.projectmaintainer.ui.views.git;

import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectItem {

    ProjectOperationState operationState = null;
    Project project;
    Optional<Image> image;
    Optional<Commit> latestCommit;
    String text = "";
    String owner;
    boolean operationInProgress;
    Double operationProgressValue;

    public String getNamePrefix() {
        return this.project.getMetaData().getFQPN().getSegments()
                .stream()
                .skip(1)
                .filter(s -> !s.equals(this.project.getMetaData().getName()))
                .collect(Collectors.joining(" / "));
    }

    public String getName() {
        return this.getProject().getMetaData().getName();
    }
}

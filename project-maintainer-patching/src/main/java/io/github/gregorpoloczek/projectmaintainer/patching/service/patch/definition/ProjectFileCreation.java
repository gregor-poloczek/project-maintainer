package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectFileCreation extends ProjectFileOperation {

    @NonNull
    String content;

    public ProjectFileCreation(
            ProjectFileLocation location,
            @NonNull String content) {
        super(ProjectFileOperationType.ADD, location, null, content);
        this.content = content;
    }
}

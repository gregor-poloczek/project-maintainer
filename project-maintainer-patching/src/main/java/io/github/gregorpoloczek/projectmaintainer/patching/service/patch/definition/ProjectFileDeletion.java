package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectFileDeletion extends ProjectFileOperation {

    public ProjectFileDeletion(
            ProjectFileLocation location, String before) {
        super(ProjectFileOperationType.DELETE, location, before, null);
    }
}

package de.gregorpoloczek.projectmaintainer.patching.service.patch.definition;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectFileUpdate extends ProjectFileOperation {

    public ProjectFileUpdate(
            ProjectFileLocation location,
            String before,
            @NonNull String after) {
        super(ProjectFileOperationType.UPDATE, location, before, after);
    }

}

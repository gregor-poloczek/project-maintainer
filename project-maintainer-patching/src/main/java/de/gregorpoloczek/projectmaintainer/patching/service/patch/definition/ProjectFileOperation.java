package de.gregorpoloczek.projectmaintainer.patching.service.patch.definition;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ProjectFileOperation {

    ProjectFileOperationType type;
    ProjectFileLocation location;
    String before;
    String after;

    public Optional<String> getAfter() {
        return Optional.ofNullable(after);
    }

    public Optional<String> getBefore() {
        return Optional.ofNullable(before);
    }
}

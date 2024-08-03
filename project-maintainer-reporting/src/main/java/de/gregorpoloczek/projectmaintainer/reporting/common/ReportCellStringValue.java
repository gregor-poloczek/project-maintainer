package de.gregorpoloczek.projectmaintainer.reporting.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Builder
@Getter
public class ReportCellStringValue implements ReportCellValue {

    ProjectFileLocation location;
    @NonNull
    String stringValue;

    public Optional<ProjectFileLocation> getLocation() {
        return Optional.ofNullable(location);
    }

}

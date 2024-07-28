package de.gregorpoloczek.projectmaintainer.reporting.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Builder
public class ReportCellBooleanValue implements ReportCellValue {

    ProjectFileLocation location;
    boolean booleanValue;


    @Override
    public String getStringValue() {
        return Boolean.toString(booleanValue);
    }

    public Optional<ProjectFileLocation> getLocation() {
        return Optional.ofNullable(location);
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }
}

package io.github.gregorpoloczek.projectmaintainer.reporting.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
@Builder
public class ReportCellErrorValue implements ReportCellValue {

    @NonNull
    String message;

    @Override
    public String getStringValue() {
        return this.message;
    }

    @Override
    public Optional<ProjectFileLocation> getLocation() {
        return Optional.empty();
    }
}

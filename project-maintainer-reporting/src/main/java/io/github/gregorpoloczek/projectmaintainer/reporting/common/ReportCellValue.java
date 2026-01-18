package io.github.gregorpoloczek.projectmaintainer.reporting.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.util.Optional;

public interface ReportCellValue {

    String getStringValue();

    Optional<ProjectFileLocation> getLocation();
}

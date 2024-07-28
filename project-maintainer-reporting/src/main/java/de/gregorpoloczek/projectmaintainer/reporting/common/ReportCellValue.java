package de.gregorpoloczek.projectmaintainer.reporting.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.util.Optional;

public interface ReportCellValue {

    String getStringValue();

    Optional<ProjectFileLocation> getLocation();
}

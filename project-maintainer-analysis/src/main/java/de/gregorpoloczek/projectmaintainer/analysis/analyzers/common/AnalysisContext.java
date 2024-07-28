package de.gregorpoloczek.projectmaintainer.analysis.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;

public interface AnalysisContext {

    Project getProject();

    ProjectFiles files();

    FactsCollector facts();

    FactsCollector facts(ProjectFileLocation location);
}

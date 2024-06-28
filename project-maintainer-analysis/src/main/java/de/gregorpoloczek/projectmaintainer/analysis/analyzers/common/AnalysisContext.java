package de.gregorpoloczek.projectmaintainer.analysis.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;

public interface AnalysisContext {

    Project getProject();

    ProjectFiles files();

    FactsCollector facts();
}

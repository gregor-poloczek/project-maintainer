package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.FactsCollector;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;

public interface AnalysisContext {

  Project getProject();

  ProjectFiles files();

  FactsCollector facts();
}

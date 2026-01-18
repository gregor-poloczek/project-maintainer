package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;

public interface AnalysisContext {

    Project getProject();

    ProjectFiles files();

    FactsCollector facts();

    FactsCollector facts(ProjectFileLocation location);
}

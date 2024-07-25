package de.gregorpoloczek.projectmaintainer.analysis.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import java.io.File;

public interface AnalysisContext {

    Project getProject();

    ProjectFiles files();

    FactsCollector facts();

    FactsCollector facts(File file);
}

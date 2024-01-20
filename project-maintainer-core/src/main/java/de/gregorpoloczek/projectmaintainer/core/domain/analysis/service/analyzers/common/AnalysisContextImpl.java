package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.FactsCollector;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;

public class AnalysisContextImpl implements AnalysisContext {

  private final Project project;

  public AnalysisContextImpl(final Project project) {
    this.project = project;
  }

  @Override
  public Project getProject() {
    return this.project;
  }

  @Override
  public ProjectFiles files() {
    return new ProjectFilesImpl(project);
  }

  @Override
  public FactsCollector facts() {
    return project.facts();
  }
}

package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.AnalysisContextImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectAnalysisService {

  private final ProjectService projectService;
  private final List<ProjectAnalyzer> projectAnalyzers;

  public ProjectAnalysisService(final ProjectService projectService,
      final List<ProjectAnalyzer> projectAnalyzers) {
    this.projectService = projectService;
    this.projectAnalyzers = projectAnalyzers;
  }


  public void analyze(FQPN fqpn, ProjectOperationProgressListener listener) {
    final Project project = projectService.getProject(fqpn)
        .orElseThrow(() -> new ProjectNotFoundException(fqpn));

    for (ProjectAnalyzer analyzer : this.projectAnalyzers) {
      listener.update(analyzer.getClass().getSimpleName());
      try {
        analyzer.analyze(new AnalysisContextImpl(project));
      } catch (RuntimeException e) {
        log.error("Could not invoke analyzer %s on project %s".formatted(
            analyzer.getClass().getSimpleName(), project.getFQPN()));
      }
    }

    listener.succeeded(project);
  }
}

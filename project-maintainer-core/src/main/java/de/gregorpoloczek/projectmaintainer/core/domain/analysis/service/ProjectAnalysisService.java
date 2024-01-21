package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.AnalysisContextImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectAnalysisService {

  private final ProjectService projectService;
  private final LabelService labelService;
  private final WorkingCopyService workingCopyService;
  private final List<ProjectAnalyzer> projectAnalyzers;

  public ProjectAnalysisService(
      final ProjectService projectService,
      final LabelService labelService,
      final List<ProjectAnalyzer> projectAnalyzers,
      final WorkingCopyService workingCopyService
  ) {
    this.projectService = projectService;
    this.labelService = labelService;
    this.projectAnalyzers = projectAnalyzers;
    this.workingCopyService = workingCopyService;
  }


  public void analyze(@NonNull FQPN fqpn, @NonNull ProjectOperationProgressListener listener) {
    final Project project = projectService.getProject(fqpn)
        .orElseThrow(() -> new ProjectNotFoundException(fqpn));

    final Optional<WorkingCopy> maybeWorkingCopy =
        workingCopyService.find(fqpn);

    if (!maybeWorkingCopy.isPresent()) {
      listener.failed(project, new ProjectNotClonedException(fqpn));
      return;
    }

    final WorkingCopy workingCopy = maybeWorkingCopy.get();

    try {
      project.withReadLock(() -> {
        final AnalysisContextImpl context = new AnalysisContextImpl(project, workingCopy);
        this.performAnalysis(context, listener);
        this.saveAnalysisResult(context);
        return null;
      });

      // TODO kann fehlschlagen
      listener.succeeded(project);
    } catch (RuntimeException e) {
      log.error("Unexpected error during project analysis of \"%s\".".formatted(fqpn), e);
      listener.failed(project, e);
    }
  }

  private void performAnalysis(final AnalysisContextImpl context,
      final ProjectOperationProgressListener listener) {
    final Project project = context.getProject();

    double i = 0.0d;
    for (ProjectAnalyzer analyzer : this.projectAnalyzers) {
      listener.update(analyzer.getClass().getSimpleName(),
          i / (double) this.projectAnalyzers.size());
      try {
        analyzer.analyze(context);
      } catch (RuntimeException e) {
        log.error("Could not invoke analyzer %s on project %s".formatted(
            analyzer.getClass().getSimpleName(), project.getFQPN()), e);
      }
      i += 1.0d;
    }
  }

  private void saveAnalysisResult(final AnalysisContextImpl context) {
    final Project project = context.getProject();
    this.labelService.save(project.getFQPN(), context.getLabels());
  }
}

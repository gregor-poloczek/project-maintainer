package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.AnalysisContextImpl;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.git.service.ProjectNotClonedException;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ProjectAnalysisService {

    private final ProjectService projectService;
    private final LabelService labelService;
    private final WorkingCopyService workingCopyService;
    private final List<ProjectAnalyzer> projectAnalyzers;
    private final DependencyService dependencyService;
    private Map<FQPN, String> lastAnalyzedCommitHash = Collections.synchronizedMap(new HashMap<>());

    public ProjectAnalysisService(
            final ProjectService projectService,
            final List<ProjectAnalyzer> projectAnalyzers,
            final WorkingCopyService workingCopyService,
            final LabelService labelService,
            final DependencyService dependencyService
    ) {
        this.projectService = projectService;
        this.labelService = labelService;
        this.projectAnalyzers = projectAnalyzers;
        this.workingCopyService = workingCopyService;
        this.dependencyService = dependencyService;
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
                String latestHash = workingCopy.getLatestCommit().map(Commit::getHash).orElse("NO-HASH");
                if (Objects.equals(latestHash,
                        this.lastAnalyzedCommitHash.get(context.getProject().getMetaData().getFQPN()))) {
                    listener.succeeded(context.getProject());
                    return null;
                }

                this.performAnalysis(context);
                this.saveAnalysisResult(context, latestHash);
                return null;
            });

            // TODO kann fehlschlagen
            listener.succeeded(project);
        } catch (RuntimeException e) {
            log.error("Unexpected error during project analysis of \"%s\".".formatted(fqpn), e);
            listener.failed(project, e);
        }
    }

    public Mono<Void> analyze(@NonNull FQPN fqpn) {
        final Project project = projectService.getProject(fqpn)
                .orElseThrow(() -> new ProjectNotFoundException(fqpn));

        final Optional<WorkingCopy> maybeWorkingCopy =
                workingCopyService.find(fqpn);

        if (!maybeWorkingCopy.isPresent()) {
            return Mono.error(new ProjectNotClonedException(fqpn));
        }

        return Mono.fromCallable(() -> {
            final WorkingCopy workingCopy = maybeWorkingCopy.get();

            try {
                log.info("Analyzing project \"{}\".", fqpn);
                return project.<Void>withReadLock(() -> {
                    final AnalysisContextImpl context = new AnalysisContextImpl(project, workingCopy);
                    String latestHash = workingCopy.getLatestCommit().map(Commit::getHash).orElse("NO-HASH");
                    if (Objects.equals(latestHash,
                            this.lastAnalyzedCommitHash.get(context.getProject().getMetaData().getFQPN()))) {
                        return null;
                    }

                    this.performAnalysis(context);
                    this.saveAnalysisResult(context, latestHash);
                    return null;
                });
            } catch (RuntimeException e) {
                log.error("Unexpected error during project analysis of \"%s\".".formatted(fqpn), e);
                throw e;
            }
        });

    }


    private void performAnalysis(final AnalysisContextImpl context) {
        final Project project = context.getProject();

        double i = 0.0d;
        for (ProjectAnalyzer analyzer : this.projectAnalyzers) {
            try {
                analyzer.analyze(context);
            } catch (RuntimeException e) {
                log.error("Could not invoke analyzer %s on project %s".formatted(
                        analyzer.getClass().getSimpleName(), project.getMetaData().getFQPN()), e);
            }
            i += 1.0d;
        }
    }

    private void saveAnalysisResult(final AnalysisContextImpl context, String latestHash) {
        final Project project = context.getProject();
        this.labelService.save(project.getMetaData().getFQPN(), context.getLabels());
        this.dependencyService.save(project.getMetaData().getFQPN(), context.getDependencies());
        this.lastAnalyzedCommitHash.put(context.getProject().getMetaData().getFQPN(), latestHash);
    }
}

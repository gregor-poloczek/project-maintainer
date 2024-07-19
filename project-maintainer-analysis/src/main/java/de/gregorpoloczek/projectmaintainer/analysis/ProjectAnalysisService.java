package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisProgress.State;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.AnalysisContextImpl;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Service
@Slf4j
public class ProjectAnalysisService {

    private final ProjectService projectService;
    private final LabelService labelService;
    private final WorkingCopyService workingCopyService;
    private final List<ProjectAnalyzer> projectAnalyzers;
    private final DependencyService dependencyService;
    private final Map<FQPN, String> lastAnalyzedCommitHash = Collections.synchronizedMap(new HashMap<>());

    public ProjectAnalysisService(
            final ProjectService projectService,
            final WorkingCopyService workingCopyService,
            final LabelService labelService,
            final DependencyService dependencyService,
            final List<ProjectAnalyzer> projectAnalyzers
    ) {
        this.projectService = projectService;
        this.labelService = labelService;
        this.workingCopyService = workingCopyService;
        this.dependencyService = dependencyService;
        this.projectAnalyzers = projectAnalyzers;
    }


    public Flux<ProjectAnalysisProgress> analyze(@NonNull ProjectRelatable projectRelatable) {
        final Optional<Project> maybeProject = projectService.getProject(projectRelatable);
        if (maybeProject.isEmpty()) {
            return Flux.error(new ProjectNotFoundException(projectRelatable));
        }

        final Optional<WorkingCopy> maybeWorkingCopy = workingCopyService.find(projectRelatable);
        if (maybeWorkingCopy.isEmpty()) {
            return Flux.error(new ProjectNotClonedException(projectRelatable));
        }
        final WorkingCopy workingCopy = maybeWorkingCopy.get();

        return Flux.create(sink -> {
            FQPN fqpn = projectRelatable.getFQPN();

            try {
                log.debug("Analyzing project \"{}\".", fqpn);
                Project project = maybeProject.get();
                sink.next(ProjectAnalysisProgress.builder()
                        .fqpn(fqpn).state(State.SCHEDULED)
                        .build());
                project.<Void>withReadLock(() -> {
                    final AnalysisContextImpl context = new AnalysisContextImpl(project, workingCopy);
                    String latestHash = workingCopy.getLatestCommit().map(Commit::getHash).orElse("NO-HASH");
                    if (Objects.equals(latestHash,
                            this.lastAnalyzedCommitHash.get(context.getProject().getMetaData().getFQPN()))) {
                        sink.next(ProjectAnalysisProgress.builder()
                                .fqpn(fqpn)
                                .state(State.DONE)
                                .build());
                        sink.complete();
                        return null;
                    }

                    this.performAnalysis(context, sink);
                    this.saveAnalysisResult(context, latestHash);
                    sink.next(ProjectAnalysisProgress.builder()
                            .fqpn(fqpn)
                            .state(State.DONE)
                            .progressCurrent(1)
                            .progressTotal(1)
                            .build());
                    sink.complete();
                    return null;
                });
            } catch (RuntimeException e) {
                log.error("Unexpected error during project analysis of \"%s\".".formatted(fqpn), e);
                sink.next(ProjectAnalysisProgress.builder()
                        .fqpn(fqpn).state(State.FAILED).build());
                sink.error(e);
            }
        });

    }


    private void performAnalysis(
            final AnalysisContextImpl context,
            FluxSink<ProjectAnalysisProgress> sink) {
        final Project project = context.getProject();

        sink.next(ProjectAnalysisProgress.builder()
                .fqpn(context.getProject().getFQPN())
                .state(State.RUNNING)
                .progressCurrent(0)
                .progressTotal(this.projectAnalyzers.size())
                .build());
        int i = 0;
        for (ProjectAnalyzer analyzer : this.projectAnalyzers) {
            try {
                analyzer.analyze(context);
            } catch (RuntimeException e) {
                log.error("Could not invoke analyzer %s on project %s".formatted(
                        analyzer.getClass().getSimpleName(), project.getMetaData().getFQPN()), e);
            }

            i++;
            final int current = i;
            sink.next(ProjectAnalysisProgress.builder()
                    .fqpn(context.getProject().getFQPN())
                    .state(State.RUNNING)
                    .progressCurrent(current)
                    .progressTotal(this.projectAnalyzers.size())
                    .build());
        }
    }

    private void saveAnalysisResult(final AnalysisContextImpl context, String latestHash) {
        this.labelService.save(context, context.getLabels());
        this.dependencyService.save(context, context.getDependencies());
        this.lastAnalyzedCommitHash.put(context.getFQPN(), latestHash);
    }
}

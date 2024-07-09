package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService.ProjectAnalysisProgress.State;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.AnalysisContextImpl;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.ProjectOperationProgressListener;
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
import lombok.Builder;
import lombok.Getter;
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

    @Getter
    @Builder
    public static class ProjectAnalysisProgress {

        private final FQPN fqpn;
        @Builder.Default
        private int progressCurrent = 0;
        @Builder.Default
        private int progressTotal = 1;

        public FQPN getFQPN() {
            return fqpn;
        }

        private final State state;

        public enum State {
            SCHEDULED, RUNNING, DONE, FAILED;

            public boolean isTerminated() {
                return this == State.DONE || this == State.FAILED;
            }
        }

    }


    public Flux<ProjectAnalysisProgress> analyze(@NonNull ProjectRelatable projectRelatable) {
        FQPN fqpn = projectRelatable.getFQPN();
        final Optional<Project> maybeProject = projectService.getProject(fqpn);
        if (maybeProject.isEmpty()) {
            return Flux.error(new ProjectNotFoundException(fqpn));
        }

        final Optional<WorkingCopy> maybeWorkingCopy = workingCopyService.find(fqpn);
        if (maybeWorkingCopy.isEmpty()) {
            return Flux.error(new ProjectNotClonedException(fqpn));
        }

        return Flux.create(sink -> {
            final WorkingCopy workingCopy = maybeWorkingCopy.get();

            try {
                log.info("Analyzing project \"{}\".", fqpn);
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

                    this.performAnalysis(context, Optional.of(sink));
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


    private void performAnalysis(final AnalysisContextImpl context, Optional<FluxSink<ProjectAnalysisProgress>> sink) {
        final Project project = context.getProject();

        sink.ifPresent(s -> s.next(ProjectAnalysisProgress.builder()
                .fqpn(context.getProject().getFQPN())
                .state(State.RUNNING)
                .progressCurrent(0)
                .progressTotal(this.projectAnalyzers.size())
                .build()));
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
            sink.ifPresent(s -> s.next(ProjectAnalysisProgress.builder()
                    .fqpn(context.getProject().getFQPN())
                    .state(State.RUNNING)
                    .progressCurrent(current)
                    .progressTotal(this.projectAnalyzers.size())
                    .build()));
        }
    }

    private void saveAnalysisResult(final AnalysisContextImpl context, String latestHash) {
        final Project project = context.getProject();
        this.labelService.save(project.getMetaData().getFQPN(), context.getLabels());
        this.dependencyService.save(project.getMetaData().getFQPN(), context.getDependencies());
        this.lastAnalyzedCommitHash.put(context.getProject().getMetaData().getFQPN(), latestHash);
    }
}

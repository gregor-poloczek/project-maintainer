package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.AnalysisContextImpl;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.analysis.fulltext.ProjectFullTextSearchService;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectAnalysisService {

    private final ProjectService projectService;
    private final LabelService labelService;
    private final WorkingCopyService workingCopyService;
    private final List<ProjectAnalyzer> projectAnalyzers;
    private final DependencyService dependencyService;
    private final Map<FQPN, String> lastAnalyzedCommitHash = Collections.synchronizedMap(new HashMap<>());
    private final ProjectFullTextSearchService projectFullTextSearchService;

    public Flux<ProjectOperationProgress<Void>> analyze(@NonNull ProjectRelatable projectRelatable) {
        return Flux.create(sink -> {
            Project project = projectService.requireProject(projectRelatable);
            WorkingCopy workingCopy = workingCopyService.require(projectRelatable);
            FQPN fqpn = projectRelatable.getFQPN();

            try {
                log.debug("Analyzing project \"{}\".", fqpn);
                sink.next(ProjectOperationProgress.<Void>builder()
                        .fqpn(fqpn)
                        .state(OperationProgress.State.SCHEDULED)
                        .build());
                // TODO possible thread starvation?
                project.<Void>withReadLock(() -> {
                    final AnalysisContextImpl context = new AnalysisContextImpl(project, workingCopy);
                    String latestHash = workingCopy.getLatestCommit().map(Commit::getHash).orElse("NO-HASH");
                    if (Objects.equals(latestHash,
                            this.lastAnalyzedCommitHash.get(context.getProject().getMetaData().getFQPN()))) {
                        sink.next(ProjectOperationProgress.<Void>builder()
                                .fqpn(fqpn)
                                .state(OperationProgress.State.DONE)
                                .build());
                        sink.complete();
                        return null;
                    }

                    this.performAnalysis(context, sink);
                    this.saveAnalysisResult(context, latestHash);
                    sink.next(ProjectOperationProgress.<Void>builder()
                            .fqpn(fqpn)
                            .state(OperationProgress.State.DONE)
                            .progressCurrent(1)
                            .progressTotal(1)
                            .build());
                    sink.complete();
                    return null;
                });
            } catch (RuntimeException e) {
                log.error("Unexpected error during project analysis of \"%s\".".formatted(fqpn), e);
                sink.next(ProjectOperationProgress.<Void>builder()
                        .fqpn(fqpn).state(OperationProgress.State.FAILED).build());
                sink.error(e);
            }
        });

    }


    private void performAnalysis(
            final AnalysisContextImpl context,
            FluxSink<ProjectOperationProgress<Void>> sink) {
        final Project project = context.getProject();

        sink.next(ProjectOperationProgress.<Void>builder()
                .fqpn(context.getProject().getFQPN())
                .state(OperationProgress.State.RUNNING)
                .progressCurrent(0)
                .progressTotal(this.projectAnalyzers.size())
                .build());

        List<ProjectFileLocation> locations = context.files().findLocations("\\.(java|json|js|ts|groovy|html)$");
        this.projectFullTextSearchService.index(context, locations);

        int i = 0;
        for (ProjectAnalyzer analyzer : this.projectAnalyzers) {
            try {
                log.trace("Analyzing {} with {}", project.getFQPN(), analyzer.getClass().getSimpleName());
                analyzer.analyze(context);
            } catch (RuntimeException e) {
                // TODO an sink propagieren??
                log.error("Could not invoke analyzer %s on project %s".formatted(
                        analyzer.getClass().getSimpleName(), project.getMetaData().getFQPN()), e);
            }

            i++;
            final int current = i;
            sink.next(ProjectOperationProgress.<Void>builder()
                    .fqpn(context.getProject().getFQPN())
                    .state(OperationProgress.State.RUNNING)
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

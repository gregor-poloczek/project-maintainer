package io.github.gregorpoloczek.projectmaintainer.analysis.service;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.dependency.DependencyService;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.label.LabelService;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.AnalysisContextImpl;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectAnalyzer;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.ProjectFullTextSearchService;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress.ProjectOperationProgressBuilder;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.Commit;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectAnalysisService {

    ProjectService projectService;
    LabelService labelService;
    WorkingCopyService workingCopyService;
    List<ProjectAnalyzer> projectAnalyzers;
    DependencyService dependencyService;
    ProjectFullTextSearchService projectFullTextSearchService;

    Map<FQPN, String> lastAnalyzedCommitHash = Collections.synchronizedMap(new HashMap<>());

    public Flux<ProjectOperationProgress<Void>> analyze(@NonNull ProjectRelatable projectRelatable) {
        return Flux.create(sink -> {
            Project project = projectService.require(projectRelatable);
            WorkingCopy workingCopy = workingCopyService.require(projectRelatable);
            FQPN fqpn = projectRelatable.getFQPN();

            try {
                log.debug("Analyzing project \"{}\".", fqpn);
                sink.next(ProjectOperationProgress.<Void>builder()
                        .fqpn(fqpn)
                        .state(OperationProgress.State.SCHEDULED)
                        .build());
                // TODO possible thread starvation?
                workingCopy.withReadLock(() -> {
                    final AnalysisContextImpl context = new AnalysisContextImpl(project, workingCopy);
                    this.performAnalysis(context, sink);
                });
                sink.complete();
            } catch (RuntimeException e) {
                log.error("Unexpected error during project analysis of \"%s\".".formatted(fqpn), e);
                sink.next(ProjectOperationProgress.<Void>builder()
                        .fqpn(fqpn).state(OperationProgress.State.FAILED).build());
                sink.error(e);
            }
        });

    }


    private boolean performAnalysis(
            final AnalysisContextImpl context,
            FluxSink<ProjectOperationProgress<Void>> sink) {
        final Project project = context.getProject();

        int total = this.projectAnalyzers.size() + 1;
        int current = 0;

        ProjectOperationProgressBuilder<Void> progress =
                ProjectOperationProgress.<Void>builder()
                        .fqpn(context.getFQPN())
                        .progressTotal(total)
                        .progressCurrent(0);

        sink.next(progress.state(State.STARTED).progressCurrent(total).build());

        String latestHash = context.getWorkingCopy().getLatestCommit().map(Commit::getHash).orElse("NO-HASH");
        if (Objects.equals(latestHash,
                this.lastAnalyzedCommitHash.get(context.getProject().getMetaData().getFQPN()))) {
            sink.next(progress.state(State.DONE).progressCurrent(total).build());
            return false;
        }

        sink.next(progress.state(State.RUNNING).progressCurrent(current).build());

        List<ProjectFileLocation> locations = context.files().findLocations("\\.(java|json|js|ts|groovy|html)$");
        this.projectFullTextSearchService.index(context, locations);
        sink.next(progress.state(State.RUNNING).progressCurrent(current++).build());

        for (ProjectAnalyzer analyzer : this.projectAnalyzers) {
            try {
                log.trace("Analyzing {} with {}", project.getFQPN(), analyzer.getClass().getSimpleName());
                analyzer.analyze(context);
            } catch (RuntimeException e) {
                // TODO an sink propagieren??
                log.error("Could not invoke analyzer %s on project %s".formatted(
                        analyzer.getClass().getSimpleName(), project.getMetaData().getFQPN()), e);
            }

            sink.next(progress.state(State.RUNNING).progressCurrent(current++).build());
        }

        this.lastAnalyzedCommitHash.put(context.getFQPN(), latestHash);

        this.saveAnalysisResult(context);

        sink.next(progress.state(State.DONE).progressCurrent(current).build());
        return true;
    }

    private void saveAnalysisResult(final AnalysisContextImpl context) {
        this.labelService.save(context, context.getLabels());
        this.dependencyService.save(context, context.getDependencies());
    }
}

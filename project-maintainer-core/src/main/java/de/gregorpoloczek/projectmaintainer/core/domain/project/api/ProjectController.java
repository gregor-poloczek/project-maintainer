package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.ProjectResource;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/v1/projects")
@Slf4j
public class ProjectController {

  private final ProjectService projectService;

  private final Executor executor;

  private final ProjectAnalysisService projectAnalysisService;

  public ProjectController(final ProjectService projectService,
      final ProjectAnalysisService projectAnalysisService, final Executor executor) {
    this.projectService = projectService;
    this.projectAnalysisService = projectAnalysisService;
    this.executor = executor;
  }

  final Sinks.Many<ProjectOperationProgress> sink = Sinks
      .many()
      .multicast()
      .onBackpressureBuffer();

  @PostMapping("/{fqpn}/operations/wipe")
  public void wipeProject(@PathVariable("fqpn") String fpqn) {
    this.executeAsyncOperation(fpqn, "wipe", this.projectService::wipeProject);
  }

  @PostMapping("/{fqpn}/operations/clone")
  public void cloneProject(@PathVariable("fqpn") String fqpn) {
    this.executeAsyncOperation(fqpn, "clone", this.projectService::cloneProject);
  }


  @PostMapping(value = "/{fqpn}/operations/pull")
  public void pullProject(@PathVariable("fqpn") String fqpn) {
    this.executeAsyncOperation(fqpn, "pull", this.projectService::pullProject);
  }

  @PostMapping(value = "/{fqpn}/operations/analyse")
  public void analyseProject(@PathVariable("fqpn") String fqpn) {
    this.executeAsyncOperation(fqpn, "analyse", this.projectAnalysisService::analyze);
  }

  @GetMapping(value = "/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<ProjectOperationProgress>> getUpdates() {
    return sink.asFlux().map(e -> ServerSentEvent.builder(e).build());
  }

  @GetMapping("/")
  public ResponseEntity<List<ProjectResource>> getProjects() {
    final List<Project> result = this.projectService.getProjects();

    return ResponseEntity.ok(result.stream().map(ProjectResource::of).collect(
        toList()));
  }

  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  public class ResourceNotFoundException extends RuntimeException {

  }

  @GetMapping("/{fqpn}")
  public ResponseEntity<ProjectResource> getProject(@PathVariable("fqpn") String fqpn) {
    final Optional<Project> project = this.projectService.getProject(FQPN.of(fqpn));
    return project
        .map(ProjectResource::of)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResourceNotFoundException());
  }

  private void executeAsyncOperation(String fqpn, final String operationName,
      final BiConsumer<FQPN, ProjectOperationProgressListener> operation) {
    this.executeAsyncOperation(singleton(FQPN.of(fqpn)), operationName, operation);
  }

  private void executeAsyncOperation(final Collection<FQPN> fqpns, final String operationName,
      final BiConsumer<FQPN, ProjectOperationProgressListener> operation) {

    final AtomicInteger left = new AtomicInteger(fqpns.size());
    final List<Throwable> caught = Collections.synchronizedList(new ArrayList<>());
    final BiConsumer<FQPN, Optional<Throwable>> onComplete = (p, e) -> {
      e.ifPresent(caught::add);
      if (left.decrementAndGet() == 0) {
        if (caught.isEmpty()) {
          // TODO what to do?
          //sseEmitter.complete();
        } else {
          // TODO handle differently
//          sseEmitter.completeWithError(caught.get(0));
        }
      }
    };

    for (FQPN fqpn : fqpns) {
      final ProjectOperationProgressListener emitter =
          new SinkBasedProjectOperationProgressListener(this.sink, fqpn, operationName,
              onComplete);
      emitter.scheduled();

      this.executor.execute(
          () -> {
            final Project project = projectService.getProject(fqpn).get();
            try {
              operation.accept(fqpn, emitter);
              emitter.succeeded(project);
            } catch (RuntimeException e) {
              emitter.failed(projectService.getProject(fqpn).get(), e);
            }
          }
      );
    }
  }

}

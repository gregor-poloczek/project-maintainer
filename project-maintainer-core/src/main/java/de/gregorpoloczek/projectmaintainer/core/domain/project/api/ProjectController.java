package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/v1/projects")
@Slf4j
public class ProjectController {

  private final ProjectService projectService;

  private final Executor executor;

  public ProjectController(final ProjectService projectService, final Executor executor) {
    this.projectService = projectService;
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

  @PostMapping(value = "/operations/wipe")
  public void wipeProjects() {
    this.executeAsyncOperation("wipe", this.projectService::wipeProject);
  }

  @PostMapping(value = "/operations/clone")
  public void cloneProjects() {
    this.executeAsyncOperation("clone", this.projectService::cloneProject);
  }

  @PostMapping(value = "/operations/pull")
  public void pullProjects() {
    this.executeAsyncOperation("pull", this.projectService::pullProject);
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

  private SseEmitter executeAsyncOperation(final String operationName,
      final BiConsumer<FQPN, ProjectOperationProgressListener> operation) {
    final List<FQPN> fqpns =
        this.projectService.getProjects().stream().map(Project::getFQPN)
            .collect(toList());
    return this.executeAsyncOperation(fqpns, operationName, operation);
  }

  private SseEmitter executeAsyncOperation(String fqpn, final String operationName,
      final BiConsumer<FQPN, ProjectOperationProgressListener> operation) {
    return this.executeAsyncOperation(singleton(FQPN.of(fqpn)), operationName, operation);
  }

  private SseEmitter executeAsyncOperation(final Collection<FQPN> fqpns, final String operationName,
      final BiConsumer<FQPN, ProjectOperationProgressListener> operation) {
    //final SseEmitter sseEmitter = new SseEmitter();

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
            operation.accept(fqpn, emitter);
          }
      );
    }
    return null;
  }

}

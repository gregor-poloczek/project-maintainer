package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.ProjectResource;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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


  @PostMapping(value = "/{fqpn}/operations/wipe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter wipeProject(@PathVariable("fqpn") String fpqn) {
    return this.executeAsyncOperation(fpqn, "wipe", this.projectService::wipeProject);
  }

  @PostMapping(value = "/{fqpn}/operations/clone", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter cloneProject(@PathVariable("fqpn") String fqpn) {
    return this.executeAsyncOperation(fqpn, "clone", this.projectService::cloneProject);
  }

  @PostMapping(value = "/{fqpn}/operations/pull", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter pullProject(@PathVariable("fqpn") String fqpn) {
    return this.executeAsyncOperation(fqpn, "pull", this.projectService::pullProject);
  }

  @GetMapping(value = "/operations/wipe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter wipeProjects() {
    return this.executeAsyncOperation("wipe", this.projectService::wipeProject);
  }

  @GetMapping(value = "/operations/clone", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter cloneProjects() {
    return this.executeAsyncOperation("clone", this.projectService::cloneProject);
  }

  @GetMapping(value = "/operations/pull", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter pullProjects() {
    return this.executeAsyncOperation("pull", this.projectService::pullProject);
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
    final SseEmitter sseEmitter = new SseEmitter();

    final AtomicInteger left = new AtomicInteger(fqpns.size());
    final List<Throwable> caught = Collections.synchronizedList(new ArrayList<>());
    final BiConsumer<FQPN, Optional<Throwable>> onComplete = (p, e) -> {
      e.ifPresent(caught::add);
      if (left.decrementAndGet() == 0) {
        if (caught.isEmpty()) {
          sseEmitter.complete();
        } else {
          // TODO handle differently
          sseEmitter.completeWithError(caught.get(0));
        }
      }
    };

    for (FQPN fqpn : fqpns) {
      final ProjectOperationProgressListener emitter =
          new SseEmitterBasedProjectOperationProgressListener(sseEmitter, fqpn, operationName,
              onComplete);
      emitter.scheduled();
      this.executor.execute(
          () -> {
            operation.accept(fqpn, emitter);
          }
      );
    }
    return sseEmitter;
  }

}

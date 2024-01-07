package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.ProjectResource;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneResult;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.PullResult;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
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

@RestController
@RequestMapping("/v1/projects")
@Slf4j
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(final ProjectService projectService) {
    this.projectService = projectService;
  }

  @PostMapping("/operations/clone")
  public Flux<ServerSentEvent<CloneResult>> clone() {
    return this.projectService.cloneProjects().map(r -> ServerSentEvent.builder(r).build());
  }

  @PostMapping("/operations/pull")
  public Flux<ServerSentEvent<PullResult>> pull() {
    return this.projectService.pullProjects().map(r -> ServerSentEvent.builder(r).build());
  }

  public static class ProjectOperationProgressEmitter {

    @Getter
    private SseEmitter emitter = new SseEmitter();

    public void send(OperationProgress progress) {
      try {
        emitter.send(
            SseEmitter.event()
                .id(progress.getOperation())
                .name(progress.getState().name())
                .data(progress, MediaType.APPLICATION_JSON));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public void complete() {
      this.emitter.complete();
    }
  }

  @PostMapping(value = "/{fqpn}/operations/wipe")
  public SseEmitter wipeProject(@PathVariable("fqpn") String rawFQPN) {
    final FQPN fqpn = FQPN.of(rawFQPN);
    final ProjectOperationProgressEmitter emitter = new ProjectOperationProgressEmitter();
    ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();
    final OperationProgress progress = new OperationProgress(fqpn, "wipe");

    emitter.send(progress.with(OperationState.SCHEDULED));
    sseMvcExecutor.execute(() -> {
      emitter.send(progress.with(OperationState.STARTED));
      try {
        this.projectService.wipeProject(fqpn);
        emitter.send(progress.with(OperationState.SUCCEEDED));
      } catch (Exception e) {
        emitter.send(progress.with(OperationState.FAILED));
      }
      emitter.complete();
    });
    return emitter.getEmitter();
  }

  @PostMapping(value = "/{fqpn}/operations/clone", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter cloneProject(@PathVariable("fqpn") String fqpn) {
    SseEmitter emitter = new SseEmitter();

    ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();
    sseMvcExecutor.execute(() -> {
      this.projectService.cloneProject(
          FQPN.of(fqpn),
          new CloneListener() {
            @Override
            public void update(CloneProgress cloneProgress) {
              try {
                emitter.send(SseEmitter.event()
                    .data(cloneProgress, MediaType.APPLICATION_JSON));
              } catch (IOException e) {
                log.warn("Could not send server even", e);
              }
            }

            public void complete() {
              emitter.complete();
            }

            @Override
            public void fail(final Throwable e) {
              emitter.completeWithError(e);
            }
          }
      );
    });
    return emitter;
  }


  @GetMapping("/")
  public ResponseEntity<List<ProjectResource>> getProjects() {
    final List<Project> result = this.projectService.getProjects();

    return ResponseEntity.ok(result.stream().map(ProjectResource::of).collect(
        toList()));
  }
}

package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.ProjectResource;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneResult;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.PullResult;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
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

  @PostMapping(value = "/operations/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter test() {
    SseEmitter emitter = new SseEmitter();

    ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();
    sseMvcExecutor.execute(() -> {
      this.projectService.cloneProjects2(
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
//    new Thread(() -> {
//      for (int i = 0; i < 10; i++) {
//        try {
//          emitter.send(new BlubResult(new Random().nextInt()));
//          Thread.sleep(250);
//        } catch (IOException | InterruptedException e) {
//          emitter.completeWithError(e);
//        }
//      }
//      emitter.complete();
//    }).start();
    return emitter;
  }


  @GetMapping("/")
  public ResponseEntity<List<ProjectResource>> getProjects() {
    final List<Project> result = this.projectService.getProjects();

    return ResponseEntity.ok(result.stream().map(ProjectResource::of).collect(
        toList()));
  }
}

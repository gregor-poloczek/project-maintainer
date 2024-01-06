package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.ProjectResource;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneResult;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.PullResult;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1/projects")
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


  @GetMapping("/")
  public ResponseEntity<List<ProjectResource>> getProjects() {
    final List<Project> result = this.projectService.getProjects();

    return ResponseEntity.ok(result.stream().map(ProjectResource::of).collect(
        toList()));
  }
}

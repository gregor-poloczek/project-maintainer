package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.ProjectResource;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.List;
import java.util.Optional;
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

@RestController
@RequestMapping("/v1/projects")
@Slf4j
public class ProjectController {

  private final ProjectService projectService;
  private final ProjectAnalysisService projectAnalysisService;
  private final OperationExecutionService operationExecutionService;

  public ProjectController(final ProjectService projectService,
      final ProjectAnalysisService projectAnalysisService,
      final OperationExecutionService operationExecutionService) {
    this.projectService = projectService;
    this.projectAnalysisService = projectAnalysisService;
    this.operationExecutionService = operationExecutionService;
  }

  @PostMapping("/{fqpn}/operations/wipe")
  public void wipeProject(@PathVariable("fqpn") String fqpn) {
    this.operationExecutionService.executeAsyncOperation(
        this.requireProject(FQPN.of(fqpn)),
        "git::wipe",
        this.projectService::wipeProject);
  }


  @PostMapping("/{fqpn}/operations/clone")
  public void cloneProject(@PathVariable("fqpn") String fqpn) {
    this.operationExecutionService.executeAsyncOperation(
        this.requireProject(FQPN.of(fqpn)),
        "git::clone",
        this.projectService::cloneProject);
  }


  @PostMapping(value = "/{fqpn}/operations/pull")
  public void pullProject(@PathVariable("fqpn") String fqpn) {
    this.operationExecutionService.executeAsyncOperation(
        this.requireProject(FQPN.of(fqpn)),
        "git::pull",
        this.projectService::pullProject);
  }

  @PostMapping(value = "/{fqpn}/operations/analyze")
  public void analyseProject(@PathVariable("fqpn") String fqpn) {
    this.operationExecutionService.executeAsyncOperation(
        this.requireProject(FQPN.of(fqpn)),
        "analyze",
        this.projectAnalysisService::analyze);
  }


  @GetMapping(value = "/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<ProjectOperationProgress>> getUpdates() {
    return this.operationExecutionService.getUpdateEvents()
        .map(e -> ServerSentEvent.builder(e).build());
  }

  private Project requireProject(final FQPN fqpn) {
    return this.projectService.getProject(fqpn)
        .orElseThrow(() -> new ProjectNotFoundException(fqpn));
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
  public ResponseEntity<ProjectResource> requireProject(@PathVariable("fqpn") String fqpn) {
    final Optional<Project> project = this.projectService.getProject(FQPN.of(fqpn));
    return project
        .map(ProjectResource::of)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResourceNotFoundException());
  }

}

package de.gregorpoloczek.projectmaintainer.core.domain.project.api;

import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.ProjectResource;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/projects")
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(final ProjectService projectService) {
    this.projectService = projectService;
  }

  @GetMapping("/")
  public ResponseEntity<List<ProjectResource>> getProjects() {
    final List<Project> result = this.projectService.getProjects();

    return ResponseEntity.ok(result.stream().map(ProjectResource::of).collect(
        toList()));
  }
}

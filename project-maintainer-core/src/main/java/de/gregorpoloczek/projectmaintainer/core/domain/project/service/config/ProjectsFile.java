package de.gregorpoloczek.projectmaintainer.core.domain.project.service.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class ProjectsFile {

  private String version;
  private List<Project> projects = new ArrayList<>();

}

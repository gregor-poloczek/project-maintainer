package de.gregorpoloczek.projectmaintainer.core.domain.project.repository.projectsfile;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class ProjectsFileJSON {

  private String version;
  private List<ProjectJSON> projects = new ArrayList<>();

  public ProjectsFileJSON(final String version) {
    this.version = version;
  }

  public ProjectsFileJSON() {
  }


}

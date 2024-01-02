package de.gregorpoloczek.projectmaintainer.core.common.properties;

import java.io.File;
import java.net.URI;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Projects {

  private File cloneDirectory = new File("./.projects");
  private List<URI> uris;
}

package de.gregorpoloczek.projectmaintainer.core.common.properties;

import java.io.File;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Projects {

  private File cloneDirectory = new File("./.projects");
  private Discovery discovery;
}

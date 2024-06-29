package de.gregorpoloczek.projectmaintainer.core.common.properties;

import java.io.File;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectsSection {

    File cloneDirectory = new File("./.projects");
}

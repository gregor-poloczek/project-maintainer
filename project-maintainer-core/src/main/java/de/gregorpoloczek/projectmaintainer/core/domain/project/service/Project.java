package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.io.File;
import java.net.URI;

public interface Project {

  File getDirectory();

  URI getUri();

  FQPN getFQPN();
}

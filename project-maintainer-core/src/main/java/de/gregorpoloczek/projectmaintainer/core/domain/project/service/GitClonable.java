package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.io.File;
import java.net.URI;

public interface GitClonable {

  URI getURI();

  File getDirectory();

  void markAsCloned();
}

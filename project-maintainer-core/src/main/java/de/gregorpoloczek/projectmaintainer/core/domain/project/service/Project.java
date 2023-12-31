package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

public interface Project {

  GitSource getSource();

  String getFQPN();
}

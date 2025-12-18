package de.gregorpoloczek.projectmaintainer.core.common.repository;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

import java.util.List;
import java.util.Optional;

public interface ProjectRelatableRepository<T> {

    Optional<T> find(ProjectRelatable projectRelatable);

    T require(ProjectRelatable projectRelatable);

    List<T> findAll();

    void save(ProjectRelatable projectRelatable, T data);

    boolean delete(ProjectRelatable projectRelatable);
}

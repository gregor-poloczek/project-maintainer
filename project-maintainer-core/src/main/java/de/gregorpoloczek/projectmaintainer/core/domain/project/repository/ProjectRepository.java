package de.gregorpoloczek.projectmaintainer.core.domain.project.repository;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ProjectRepository {

  private SortedMap<FQPN, ProjectImpl> projects = new TreeMap<>();

  public ProjectRepository() {
  }

  public List<ProjectImpl> findAll() {
    return List.copyOf(this.projects.values());
  }

  public Optional<ProjectImpl> find(final FQPN fqpn) {
    return Optional.ofNullable(this.projects.get(fqpn));
  }

  public void save(final ProjectImpl project) {
    this.projects.put(project.getFQPN(), project);
  }
}

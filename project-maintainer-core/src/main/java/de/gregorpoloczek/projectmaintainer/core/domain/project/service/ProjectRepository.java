package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.common.repository.GenericProjectRelatableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ProjectRepository extends GenericProjectRelatableRepository<ProjectImpl> {

}

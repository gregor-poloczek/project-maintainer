package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.ProjectNotClonedException;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.CloneResult;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.GitService;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.PullResult;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectRepository;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> getProjects() {
        return List.copyOf(this.projectRepository.findAll());
    }

    public ProjectService(final ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }


    public Optional<Project> getProject(@NonNull final FQPN fqpn) {
        return this.projectRepository.find(fqpn).map(Project.class::cast);
    }

    public Project requireProject(final FQPN fqpn) {
        return this.projectRepository.find(fqpn)
                .orElseThrow(() -> new ProjectNotFoundException(fqpn));
    }
}

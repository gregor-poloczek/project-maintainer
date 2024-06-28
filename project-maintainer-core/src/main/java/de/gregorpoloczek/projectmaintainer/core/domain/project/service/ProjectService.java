package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

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

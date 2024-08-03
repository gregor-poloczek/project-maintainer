package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ProjectService {

    ProjectRepository projectRepository;

    public List<Project> findALl() {
        return List.copyOf(this.projectRepository.findAll());
    }

    public Optional<Project> find(@NonNull final ProjectRelatable projectRelatable) {
        return this.projectRepository.find(projectRelatable).map(Project.class::cast);
    }

    public Project require(@NonNull final ProjectRelatable projectRelatable) {
        return this.projectRepository.require(projectRelatable);
    }
}

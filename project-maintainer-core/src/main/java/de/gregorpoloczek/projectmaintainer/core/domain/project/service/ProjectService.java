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

    private final WorkingCopyService workingCopyService;
    private final GitService gitService;
    private final ProjectRepository projectRepository;

    public List<Project> getProjects() {
        return List.copyOf(this.projectRepository.findAll());
    }

    public ProjectService(
            final WorkingCopyService workingCopyService,
            final GitService gitService,
            final ProjectRepository projectRepository
    ) {
        this.workingCopyService = workingCopyService;
        this.gitService = gitService;
        this.projectRepository = projectRepository;
    }


    public void cloneProject(@NonNull FQPN fqpn, @NonNull ProjectOperationProgressListener listener) {
        final ProjectImpl project = requireProject(fqpn);
        final WorkingCopy workingCopy =
                this.workingCopyService.createNew(project.getMetaData().getFQPN(), project.getURI(),
                        project.getCredentialsProvider());
        final CloneResult clone = this.gitService.clone(workingCopy, listener);
        this.workingCopyService.save(
                workingCopy.getFQPN(),
                workingCopy.getURI(),
                workingCopy.getDirectory(),
                clone.getLatestCommit().orElse(null),
                workingCopy.getCredentialsProvider()
        );
    }

    public void pullProject(@NonNull FQPN fqpn, @NonNull ProjectOperationProgressListener listener) {
        final WorkingCopy workingCopy = this.workingCopyService.find(fqpn)
                .orElseThrow(() -> new ProjectNotClonedException(fqpn));
        final PullResult result = this.gitService.pull(workingCopy, listener);
        this.workingCopyService.save(
                workingCopy.getFQPN(),
                workingCopy.getURI(),
                workingCopy.getDirectory(),
                result.getLatestCommit().orElse(null),
                workingCopy.getCredentialsProvider()
        );
    }


    public void wipeProject(@NonNull final FQPN fqpn,
            @NonNull final ProjectOperationProgressListener listener) {
        // TODO move code to working copy service
        final Project project = this.requireProject(fqpn);
        project.withWriteLock(() -> {
            try {
                workingCopyService.remove(fqpn);
                listener.succeeded(project);
            } catch (Exception e) {
                listener.failed(project, e);
            }
            return null;
        });
    }

    public Optional<Project> getProject(@NonNull final FQPN fqpn) {
        return this.projectRepository.find(fqpn).map(Project.class::cast);
    }

    private ProjectImpl requireProject(final FQPN fqpn) {
        return this.projectRepository.find(fqpn)
                .orElseThrow(() -> new ProjectNotFoundException(fqpn));
    }
}

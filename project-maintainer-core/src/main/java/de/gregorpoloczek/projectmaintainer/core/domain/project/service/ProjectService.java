package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectService {

    public static final String PROJECTS_FILE = "projects.json";
    public static final String SUPPORTED_VERSION = "1";
    private final ApplicationProperties applicationProperties;
    private final WorkingCopyService workingCopyService;
    private final GitService gitService;
    private final ObjectMapper objectMapper;
    private final File projectsDirectory;
    private final ProjectRepository projectRepository;
    private final File projectsFileRaw;

    public List<Project> getProjects() {
        return List.copyOf(this.projectRepository.findAll());
    }

    public ProjectService(
            final ApplicationProperties applicationProperties,
            final WorkingCopyService workingCopyService,
            final GitService gitService, final ObjectMapper objectMapper,
            final ProjectRepository projectRepository
    ) {
        this.applicationProperties = applicationProperties;
        this.workingCopyService = workingCopyService;
        this.gitService = gitService;
        this.objectMapper = objectMapper;
        this.projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
        this.projectRepository = projectRepository;
        this.projectsFileRaw = new File(projectsDirectory, PROJECTS_FILE);
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
        final ProjectImpl project = this.requireProject(fqpn);
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

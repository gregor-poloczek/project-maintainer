package de.gregorpoloczek.projectmaintainer.core.domain.bootstrap.service;

import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectRepository;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.DiscoveredProject;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryResult;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class BootstrapService {

    private final ProjectDiscoveryService projectDiscoveryService;
    private final ProjectRepository projectRepository;
    private final WorkingCopyService workingCopyService;
    private final OperationExecutionService operationExecutionService;
    private final ProjectService projectService;

    public BootstrapService(
            final ProjectDiscoveryService projectDiscoveryService,
            final ProjectRepository projectRepository,
            final WorkingCopyService workingCopyService,
            final OperationExecutionService operationExecutionService,
            final ProjectService projectService) {
        this.projectDiscoveryService = projectDiscoveryService;
        this.projectRepository = projectRepository;
        this.workingCopyService = workingCopyService;
        this.operationExecutionService = operationExecutionService;
        this.projectService = projectService;

    }

    @PostConstruct
    void init() {
        final ProjectDiscoveryResult result = this.projectDiscoveryService.discoverProjects();

        final List<DiscoveredProject> discovered = result.getDiscoveredProjects();
        log.info("Discovered {} remote projects.", discovered.size());
        discovered.forEach(p -> {
            log.info("* {}", p.getFQPN());
            log.info("  Name: {}", p.getName());
            p.getDescription().ifPresent(d -> log.info("  Description: {}", d));
            log.info("  URI: {}", p.getURI());
        });

        final List<WorkingCopy> existing = this.workingCopyService.findAll();
        log.info("Discovered {} cloned working copies.", existing.size());
        existing.forEach(w -> {
            log.info("* {}", w.getFQPN());
            log.info("  URI: {}", w.getURI());
        });

        SortedSet<FQPN> projectsToRemove = new TreeSet<>();
        projectsToRemove.addAll(existing.stream().map(WorkingCopy::getFQPN).toList());
        projectsToRemove.removeAll(discovered.stream().map(DiscoveredProject::getFQPN).toList());

        log.info("Removing {} obsolete projects", projectsToRemove.size());
        for (FQPN fqpn : projectsToRemove) {
            this.workingCopyService.remove(fqpn);
        }
        for (DiscoveredProject discoveredProject : discovered) {
            final FQPN fqpn = discoveredProject.getFQPN();

            final ProjectMetaData metaData = ProjectMetaData.builder()
                    .fqpn(fqpn)
                    .name(discoveredProject.getName())
                    .uri(discoveredProject.getURI())
                    .owner(discoveredProject.getOwner())
                    .browserLink(discoveredProject.getBrowserLink())
                    .build();

            final CredentialsProvider credentialsProvider = discoveredProject.getCredentialsProvider();

            final ProjectImpl project = new ProjectImpl(metaData, credentialsProvider);
            this.projectRepository.save(project);

//            final Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project.getMetaData().getFQPN())
//                    .map(w -> {
//                        return this.workingCopyService.save(w.getFQPN(), w.getURI(), w.getDirectory(),
//                                w.getLatestCommit().orElse(null),
//                                credentialsProvider);
//                    });

//            if (!workingCopy.isPresent()) {
//                this.operationExecutionService.executeAsyncOperation(
//                        project,
//                        "clone",
//                        this.projectService::cloneProject);
//            } else {
//                this.operationExecutionService.executeAsyncOperation(
//                        project,
//                        "pull",
//                        this.projectService::pullProject);
//            }
        }
    }

}

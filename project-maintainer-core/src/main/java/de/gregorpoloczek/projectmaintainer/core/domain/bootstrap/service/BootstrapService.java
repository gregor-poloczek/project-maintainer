package de.gregorpoloczek.projectmaintainer.core.domain.bootstrap.service;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectRepository;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.DiscoveredProject;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryResult;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import jakarta.annotation.PostConstruct;
import java.util.List;
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

    public BootstrapService(
            final ProjectDiscoveryService projectDiscoveryService,
            final ProjectRepository projectRepository,
            final WorkingCopyService workingCopyService) {
        this.projectDiscoveryService = projectDiscoveryService;
        this.projectRepository = projectRepository;
        this.workingCopyService = workingCopyService;

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
                    .description(discoveredProject.getDescription())
                    .name(discoveredProject.getName())
                    .uri(discoveredProject.getURI())
                    .owner(discoveredProject.getOwner())
                    .browserLink(discoveredProject.getBrowserLink())
                    .build();

            final CredentialsProvider credentialsProvider = discoveredProject.getCredentialsProvider();

            final ProjectImpl project = new ProjectImpl(metaData, credentialsProvider);
            this.projectRepository.save(project);
        }
    }

}

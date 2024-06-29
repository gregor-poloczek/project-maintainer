package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRepository;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DependencyService {

    private final ProjectRepository projectRepository;
    private SortedMap<FQPN, List<Dependency>> allDependencies = Collections.synchronizedSortedMap(new TreeMap<>());

    public DependencyService(final ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void save(FQPN fqpn, List<Dependency> dependencies) {
        this.allDependencies.put(fqpn, List.copyOf(dependencies));
    }

    public List<Dependency> find(FQPN fqpn) {
        return this.allDependencies.computeIfAbsent(
                this.require(fqpn).getMetaData().getFQPN(),
                k -> Collections.emptyList());
    }

    private ProjectImpl require(final FQPN fqpn) {
        return this.projectRepository.find(fqpn)
                .orElseThrow(() -> new ProjectNotFoundException(fqpn));
    }

}

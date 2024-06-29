package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DependencyService {

    private final ProjectRepository projectRepository;
    private SortedMap<FQPN, List<Dependency>> dependencyByProject
            = Collections.synchronizedSortedMap(new TreeMap<>());

    public DependencyService(final ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void save(FQPN fqpn, List<Dependency> dependencies) {
        this.dependencyByProject.put(fqpn, List.copyOf(dependencies));
    }

    public List<Dependency> find(FQPN fqpn) {
        return this.dependencyByProject.computeIfAbsent(
                this.require(fqpn).getMetaData().getFQPN(),
                k -> Collections.emptyList());
    }

    private ProjectImpl require(final FQPN fqpn) {
        return this.projectRepository.find(fqpn)
                .orElseThrow(() -> new ProjectNotFoundException(fqpn));
    }

    public SortedMap<String, SortedMap<String, SortedSet<String>>> findUsedVersions() {
        SortedMap<String, SortedMap<String, SortedSet<String>>> result = new TreeMap<>();

        this.dependencyByProject.forEach((FQPN fqpn, List<Dependency> dependencies) -> {
            for (Dependency dependency : dependencies) {
                result.computeIfAbsent(dependency.getType(), k -> new TreeMap<>())
                        .computeIfAbsent(dependency.getName(), k -> new TreeSet<>())
                        .add(dependency.getVersion().orElse("?.?.?"));
            }
        });

        return result;
    }

}

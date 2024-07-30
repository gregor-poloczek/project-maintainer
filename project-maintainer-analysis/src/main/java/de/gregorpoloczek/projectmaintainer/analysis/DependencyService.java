package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DependencyService {

    public void save(ProjectRelatable projectRelatable, List<Dependency> dependencies) {
        // noop
        //throw new NotImplementedException();
    }

    public List<Dependency> find(FQPN fqpn) {
        throw new NotImplementedException();
    }

    public SortedMap<String, SortedMap<String, SortedSet<String>>> findUsedVersions() {
        SortedMap<String, SortedMap<String, SortedSet<String>>> result = new TreeMap<>();

//        this.dependencyByProject.forEach((FQPN fqpn, List<Dependency> dependencies) -> {
//            for (Dependency dependency : dependencies) {
//                result.computeIfAbsent(dependency.getType(), k -> new TreeMap<>())
//                        .computeIfAbsent(dependency.getName(), k -> new TreeSet<>())
//                        .add(dependency.getVersion().orElse("?.?.?"));
//            }
//        });

        return result;
    }

}

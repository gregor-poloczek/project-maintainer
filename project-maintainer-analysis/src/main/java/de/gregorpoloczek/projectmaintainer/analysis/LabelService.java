package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRepository;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LabelService {

    private final ProjectRepository projectRepository;
    private SortedMap<FQPN, SortedSet<Label>> allLabels = Collections.synchronizedSortedMap(new TreeMap<>());

    public LabelService(final ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void save(ProjectRelatable projectRelatable, Collection<Label> labels) {
        FQPN fqpn = projectRelatable.getFQPN();
        final ProjectImpl project = this.require(fqpn);

        final Set<Label> finalLabels = new HashSet<>(labels);
        final List<Label> removedLabels = labels.stream()
                .filter(VersionedLabel.class::isInstance)
                .map(VersionedLabel.class::cast)
                .map(VersionedLabel::getBase)
                .filter(finalLabels::contains)
                .sorted(Comparator.comparing(Label::getValue))
                .collect(Collectors.toList());

        // Remove all labels, that are the based of actual versioned labels.
        // This can happen, when one or more analyzers produces
        // overlapping labels, such as "dep:abc" and "dep:abc:1.0.0"
        finalLabels.removeAll(removedLabels);

        if (!removedLabels.isEmpty()) {
            log.debug(
                    "Replaced labels \"{}\" from analysis of \"{}\" because versioned alternatives were provided.",
                    removedLabels, project.getMetaData().getFQPN());
        }

        this.allLabels.put(fqpn, Collections.unmodifiableSortedSet(new TreeSet<>(finalLabels)));
    }

    public SortedSet<Label> find(ProjectRelatable projectRelatable) {
        return this.allLabels.computeIfAbsent(
                this.require(projectRelatable.getFQPN()).getMetaData().getFQPN(),
                k -> Collections.emptySortedSet());
    }

    private ProjectImpl require(final FQPN fqpn) {
        return this.projectRepository.find(fqpn)
                .orElseThrow(() -> new ProjectNotFoundException(fqpn));
    }

    public SortedSet<Label> findLabelsByBase(ProjectRelatable projectRelatable, Label label) {
        return this.find(projectRelatable).stream()
                .filter(vL -> vL.getBase().equals(label)).collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean hasLabel(ProjectRelatable projectRelatable, Label label) {
        return this.find(projectRelatable).contains(label);
    }

    public boolean hasLabelsMatchingAll(Project projectRelatable, Collection<String> regularExpressions) {
        List<Pattern> patterns = regularExpressions.stream().map(Pattern::compile).toList();
        SortedSet<Label> found = this.find(projectRelatable);
        return patterns.stream().allMatch(p -> found.stream().anyMatch(l -> p.matcher(l.getValue()).matches()));
    }
}

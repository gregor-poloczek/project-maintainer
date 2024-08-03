package de.gregorpoloczek.projectmaintainer.analysis.service.label;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    public void save(ProjectRelatable projectRelatable, Collection<Label> labels) {
        FQPN fqpn = projectRelatable.getFQPN();

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
                    removedLabels, fqpn);
        }

        this.labelRepository.save(fqpn, Collections.unmodifiableSortedSet(new TreeSet<>(finalLabels)));
    }

    public SortedSet<Label> find(ProjectRelatable projectRelatable) {
        return this.labelRepository.find(projectRelatable).orElseGet(Collections::emptySortedSet);
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

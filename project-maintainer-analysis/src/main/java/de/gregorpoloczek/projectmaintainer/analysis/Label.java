package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Label implements Comparable<Label> {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final String value;
    private final List<String> segments;
    private final Label base;
    private final String lastSegment;
    private ProjectFileLocation location;

    public Optional<ProjectFileLocation> getLocation() {
        return Optional.ofNullable(location);
    }

    protected Label(final List<String> segments) {
        this.value = String.join(":", segments);
        this.segments = Collections.unmodifiableList(segments);
        this.base = segments.size() > 1 ? new Label(segments.subList(0, segments.size() - 1)) : null;
        this.lastSegment = segments.getLast();
    }

    public static Label of(String segment, String... segments) {
        final List<String> s = new ArrayList<>();
        s.add(segment);
        s.addAll(Arrays.asList(segments));
        return new Label(s);
    }

    public static Label fromString(String string) {
        // TODO validierung auf segment zahl
        return new Label(Arrays.asList(string.split(":")));
    }

    @Override
    public int compareTo(final Label o) {
        return this.value.compareTo(o.value);
    }

    public Label withLocation(ProjectFileLocation location) {
        Label label = new Label(List.copyOf(this.segments));
        label.location = location;
        return label;
    }
}

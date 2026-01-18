package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Getter
public class FQPN implements Comparable<FQPN>, ProjectRelatable, Serializable {

    public static final String SEPARATOR = "::";
    private final String value;
    private final List<String> segments;

    public FQPN(final List<String> segments) {
        this.value = segments.stream().collect(Collectors.joining(SEPARATOR));
        this.segments = segments;
    }

    public static FQPN of(String segment, String... segments) {
        final List<String> allSegments = new ArrayList<>();
        allSegments.addAll(Arrays.asList(segment.split(SEPARATOR)));
        allSegments.addAll(
                Arrays.stream(segments).map(s -> Arrays.asList(s.split(SEPARATOR))).flatMap(List::stream).toList());
        return new FQPN(allSegments);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final FQPN fqpn = (FQPN) object;

        return new EqualsBuilder().append(value, fqpn.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(value).toHashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public int compareTo(final FQPN o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public FQPN getFQPN() {
        return this;
    }

    public FQPN append(FQPN fqpn) {
        return new FQPN(Stream.of(this.segments, fqpn.segments).flatMap(List::stream).toList());
    }
}

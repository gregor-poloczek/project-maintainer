package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.exceptions.FQPNInvalidException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class FQPN implements Comparable<FQPN>, ProjectRelatable, Serializable {

    public static final String SEPARATOR = "::";
    @EqualsAndHashCode.Include
    private final String value;
    private final List<String> segments;

    public FQPN(final List<String> segments) {
        if (segments.stream().anyMatch(s -> s.contains(SEPARATOR))) {
            throw new FQPNInvalidException(segments, "segments must not contain \"%s\"".formatted(SEPARATOR));
        }
        this.value = String.join(SEPARATOR, segments);
        // the passed list is stored as is and also handed out by getSegments(); if it is mutable (e.g. the ArrayList
        // created in of(...)), modifying it afterwards makes segments and value diverge
        this.segments = segments;
    }

    public static FQPN of(String segment, String... segments) {
        final List<String> allSegments = new ArrayList<>();
        allSegments.add(segment);
        allSegments.addAll(Arrays.asList(segments));
        return new FQPN(allSegments);
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

    public FQPN append(String segment) {
        return new FQPN(Stream.of(this.segments, List.of(segment)).flatMap(List::stream).toList());
    }
}

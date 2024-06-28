package de.gregorpoloczek.projectmaintainer.core.domain.project.service;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Getter
public class FQPN implements Comparable<FQPN> {

    private final String value;
    private final List<String> segments;

    private FQPN(final List<String> segments) {
        this.value = segments.stream().collect(Collectors.joining("::"));
        this.segments = segments;
    }

    public static FQPN of(String segment, String... segments) {
        final List<String> s = new ArrayList<>();
        s.add(segment);
        s.addAll(Arrays.asList(segments));
        return new FQPN(s);
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
}

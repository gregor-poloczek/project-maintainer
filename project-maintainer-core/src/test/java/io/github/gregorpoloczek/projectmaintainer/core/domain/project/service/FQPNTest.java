package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.exceptions.FQPNInvalidException;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FQPNTest {

    @Test
    void constructor_joinsSegmentsWithSeparator() {
        FQPN fqpn = new FQPN(List.of("workspace", "connection", "project"));

        assertThat(fqpn.getSegments()).containsExactly("workspace", "connection", "project");
        assertThat(fqpn.getValue()).isEqualTo("workspace::connection::project");
        assertThat(fqpn).hasToString("workspace::connection::project");
    }

    @Test
    void constructor_rejectsSegmentsContainingSeparator() {
        List<String> segments = List.of("workspace", "group::project");

        assertThatThrownBy(() -> new FQPN(segments))
                .isInstanceOfSatisfying(FQPNInvalidException.class,
                        e -> assertThat(e.getSegments()).isEqualTo(segments));
    }

    @Test
    void of_createsFqpnFromSingleSegment() {
        FQPN fqpn = FQPN.of("project");

        assertThat(fqpn.getSegments()).containsExactly("project");
        assertThat(fqpn.getValue()).isEqualTo("project");
    }

    @Test
    void of_createsFqpnFromMultipleSegments() {
        FQPN fqpn = FQPN.of("workspace", "connection", "project");

        assertThat(fqpn.getSegments()).containsExactly("workspace", "connection", "project");
        assertThat(fqpn.getValue()).isEqualTo("workspace::connection::project");
    }

    @Test
    void of_rejectsFirstSegmentContainingSeparator() {
        assertThatThrownBy(() -> FQPN.of("workspace::connection", "project"))
                .isInstanceOf(FQPNInvalidException.class);
    }

    @Test
    void of_rejectsFollowingSegmentContainingSeparator() {
        assertThatThrownBy(() -> FQPN.of("workspace", "group::project"))
                .isInstanceOf(FQPNInvalidException.class);
    }

    @Test
    void equalsAndHashCode_areBasedOnValue() {
        FQPN fqpn = FQPN.of("workspace", "project");

        assertThat(fqpn)
                .isEqualTo(fqpn)
                .isEqualTo(FQPN.of("workspace", "project"))
                .hasSameHashCodeAs(FQPN.of("workspace", "project"))
                .isNotEqualTo(FQPN.of("workspace", "other"))
                .isNotEqualTo(FQPN.of("workspace"))
                .isNotEqualTo(null);
    }

    @Test
    void compareTo_ordersByValue() {
        FQPN a = FQPN.of("a");
        FQPN ab = FQPN.of("a", "b");
        FQPN ac = FQPN.of("a", "c");
        FQPN b = FQPN.of("b");

        assertThat(new TreeSet<>(List.of(b, ac, a, ab))).containsExactly(a, ab, ac, b);
        assertThat(a.compareTo(ab)).isNegative();
        assertThat(ab.compareTo(ac)).isNegative();
        assertThat(ac.compareTo(b)).isNegative();
        assertThat(b.compareTo(a)).isPositive();
        assertThat(ab.compareTo(FQPN.of("a", "b"))).isZero();
    }

    @Test
    void getFQPN_returnsItself() {
        FQPN fqpn = FQPN.of("workspace", "project");

        assertThat(fqpn.getFQPN()).isSameAs(fqpn);
    }

    @Test
    void appendFqpn_concatenatesSegments() {
        FQPN base = FQPN.of("workspace", "connection");

        FQPN result = base.append(FQPN.of("group", "project"));

        assertThat(result.getSegments()).containsExactly("workspace", "connection", "group", "project");
        assertThat(result).isEqualTo(FQPN.of("workspace", "connection", "group", "project"));
        // FQPNs are immutable, appending creates a new instance
        assertThat(base).isEqualTo(FQPN.of("workspace", "connection"));
    }

    @Test
    void appendString_addsSegment() {
        FQPN base = FQPN.of("workspace", "connection");

        FQPN result = base.append("project");

        assertThat(result.getSegments()).containsExactly("workspace", "connection", "project");
        assertThat(result).isEqualTo(FQPN.of("workspace", "connection", "project"));
        // FQPNs are immutable, appending creates a new instance
        assertThat(base).isEqualTo(FQPN.of("workspace", "connection"));
    }

    @Test
    void appendString_rejectsSegmentContainingSeparator() {
        FQPN base = FQPN.of("workspace");

        assertThatThrownBy(() -> base.append("group::project"))
                .isInstanceOf(FQPNInvalidException.class);
    }
}

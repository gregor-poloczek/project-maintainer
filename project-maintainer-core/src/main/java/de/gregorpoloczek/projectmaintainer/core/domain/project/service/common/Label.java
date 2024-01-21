package de.gregorpoloczek.projectmaintainer.core.domain.project.service.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Label implements Comparable<Label> {

  @Getter
  private final String value;
  @Getter
  private final List<String> segments;

  protected Label(final List<String> segments) {
    this.value = segments.stream().collect(Collectors.joining(":"));
    this.segments = Collections.unmodifiableList(segments);
  }

  public static Label of(String segment, String... segments) {
    final List<String> s = new ArrayList<>();
    s.add(segment);
    s.addAll(Arrays.asList(segments));
    return new Label(s);
  }


  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    final Label other = (Label) object;

    return new EqualsBuilder().append(value, other.value)
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
  public int compareTo(final Label o) {
    return this.value.compareTo(o.value);
  }

}

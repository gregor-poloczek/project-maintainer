package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FQPNInvalidException extends RuntimeException {
    List<String> segments;

    public FQPNInvalidException(List<String> segments, String reason) {
        super("Invalid FQPN segments %s: %s".formatted(segments, reason));
        this.segments = segments;
    }
}

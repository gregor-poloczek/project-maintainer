package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Thrown when the value of a patch parameter argument is required, but the user did not define one.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterValueMissingException extends RuntimeException {
    String parameterId;

    public PatchParameterValueMissingException(String parameterId) {
        super("No value defined for parameter \"%s\"".formatted(parameterId));
        this.parameterId = parameterId;
    }
}

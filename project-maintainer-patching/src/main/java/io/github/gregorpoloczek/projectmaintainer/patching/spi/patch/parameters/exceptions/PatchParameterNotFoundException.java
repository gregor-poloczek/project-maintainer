package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Thrown when a patch parameter argument is requested for a parameter id that is neither declared by the patch nor a
 * well known parameter provided by the application.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterNotFoundException extends RuntimeException {
    String parameterId;

    public PatchParameterNotFoundException(String parameterId) {
        super("Parameter \"%s\" not found".formatted(parameterId));
        this.parameterId = parameterId;
    }
}

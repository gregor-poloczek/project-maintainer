package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Thrown when a patch is used without passing an argument for one of the parameters it declares.
 * <p>
 * Unlike {@link PatchParameterValueMissingException}, the argument itself is missing, not just its value.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterArgumentMissingException extends RuntimeException {
    String patchId;
    String parameterId;

    public PatchParameterArgumentMissingException(String patchId, String parameterId) {
        super("No argument passed for parameter \"%s\" of patch \"%s\"".formatted(parameterId, patchId));
        this.patchId = patchId;
        this.parameterId = parameterId;
    }
}

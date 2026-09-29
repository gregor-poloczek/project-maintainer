package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Thrown when a patch parameter argument is requested as a type that does not match the type the parameter was
 * declared with, e.g. requesting a {@link PatchParameterType#INTEGER} parameter as string.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterTypeMismatchException extends RuntimeException {
    String parameterId;
    PatchParameterType declaredType;
    PatchParameterType requestedType;

    public PatchParameterTypeMismatchException(String parameterId, PatchParameterType declaredType,
                                               PatchParameterType requestedType) {
        super("Parameter \"%s\" is declared as %s, but was requested as %s".formatted(parameterId, declaredType,
                requestedType));
        this.parameterId = parameterId;
        this.declaredType = declaredType;
        this.requestedType = requestedType;
    }
}

package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterValueMissingException;

import java.util.Optional;

/**
 * The argument the user defined for a {@link PatchParameter} when applying a patch.
 * <p>
 * An argument always refers to its parameter, but does not necessarily hold a value, e.g. if the parameter is optional
 * and the user did not define it.
 *
 * @param <T> the type of the value, depending on the {@link PatchParameterType} of the parameter
 */
public interface PatchParameterArgument<T> {

    /**
     * Returns the parameter this argument was defined for.
     *
     * @return the parameter, never {@code null}
     */
    PatchParameter getParameter();

    /**
     * Returns the value the user defined for the parameter.
     *
     * @return the value, or an empty {@link Optional} if the user did not define one
     */
    Optional<T> getValue();

    /**
     * Returns the value the user defined for the parameter, for cases in which a value is mandatory.
     *
     * @return the value, never {@code null}
     * @throws PatchParameterValueMissingException if the user did not define a value
     */
    default T requireValue() {
        return this.getValue().orElseThrow(() -> new PatchParameterValueMissingException(this.getParameter().getId()));
    }

}

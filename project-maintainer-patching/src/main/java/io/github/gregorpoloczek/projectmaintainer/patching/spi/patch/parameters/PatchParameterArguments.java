package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterNotFoundException;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterTypeMismatchException;

import java.util.List;

/**
 * Gives access to patch parameter arguments defined by the user.
 * <p>
 * Arguments can be requested for every parameter the patch declares, as well as for well known parameters provided by
 * the application (e.g. the branch a patch is applied in). Each typed getter must match the {@link PatchParameterType}
 * the parameter was declared with.
 */
public interface PatchParameterArguments {

    /**
     * Returns the argument for the {@link PatchParameterType#INTEGER} parameter with the given id.
     *
     * @param parameterId the id of the parameter
     * @return the argument, never {@code null}; its value is empty if the user did not define one
     * @throws PatchParameterTypeMismatchException if the parameter is not declared as {@link PatchParameterType#INTEGER}
     * @throws PatchParameterNotFoundException     if no parameter with the given id is declared
     */
    PatchParameterArgument<Integer> getInteger(String parameterId);

    /**
     * Returns the argument for the {@link PatchParameterType#BOOLEAN} parameter with the given id.
     *
     * @param parameterId the id of the parameter
     * @return the argument, never {@code null}; its value is empty if the user did not define one
     * @throws PatchParameterTypeMismatchException if the parameter is not declared as {@link PatchParameterType#BOOLEAN}
     * @throws PatchParameterNotFoundException     if no parameter with the given id is declared
     */
    PatchParameterArgument<Boolean> getBoolean(String parameterId);

    /**
     * Returns the argument for the {@link PatchParameterType#STRING} parameter with the given id.
     *
     * @param parameterId the id of the parameter
     * @return the argument, never {@code null}; its value is empty if the user did not define one
     * @throws PatchParameterTypeMismatchException if the parameter is not declared as {@link PatchParameterType#STRING}
     * @throws PatchParameterNotFoundException     if no parameter with the given id is declared
     */
    PatchParameterArgument<String> getString(String parameterId);

    /**
     * Returns the argument for the {@link PatchParameterType#FILES} parameter with the given id.
     *
     * @param parameterId the id of the parameter
     * @return the argument, never {@code null}; its value is empty if the user did not define one, otherwise it holds
     * an unmodifiable list of the files uploaded by the user
     * @throws PatchParameterTypeMismatchException if the parameter is not declared as {@link PatchParameterType#FILES}
     * @throws PatchParameterNotFoundException     if no parameter with the given id is declared
     */
    PatchParameterArgument<List<PatchParameterFile>> getFiles(String parameterId);


    /**
     * Same as {@link #getInteger(String)}, using the id of the given parameter.
     */
    default PatchParameterArgument<Integer> getInteger(PatchParameter parameter) {
        return getInteger(parameter.getId());
    }

    /**
     * Same as {@link #getBoolean(String)}, using the id of the given parameter.
     */
    default PatchParameterArgument<Boolean> getBoolean(PatchParameter parameter) {
        return getBoolean(parameter.getId());
    }

    /**
     * Same as {@link #getString(String)}, using the id of the given parameter.
     */
    default PatchParameterArgument<String> getString(PatchParameter parameter) {
        return getString(parameter.getId());
    }

    /**
     * Same as {@link #getFiles(String)}, using the id of the given parameter.
     */
    default PatchParameterArgument<List<PatchParameterFile>> getFiles(PatchParameter parameter) {
        return getFiles(parameter.getId());
    }

    /**
     * Returns all arguments passed to the patch, regardless of their type. Parameters for which no argument was passed
     * are not included.
     *
     * @return an unmodifiable list of all arguments, never {@code null}
     */
    List<PatchParameterArgument<Object>> getAll();
}

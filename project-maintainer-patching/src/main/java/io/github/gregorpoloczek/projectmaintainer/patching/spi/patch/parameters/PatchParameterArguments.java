package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import java.util.List;

/**
 * Gives access to patch parameter arguments defined by the user.
 */
public interface PatchParameterArguments {

    PatchParameterArgument<Integer> getInteger(String parameterId);

    PatchParameterArgument<Boolean> getBoolean(String parameterId);

    PatchParameterArgument<String> getString(String parameterId);

    PatchParameterArgument<List<PatchParameterFile>> getFiles(String parameterId);


    default PatchParameterArgument<Integer> getInteger(PatchParameter parameter) {
        return getInteger(parameter.getId());
    }

    default PatchParameterArgument<Boolean> getBoolean(PatchParameter parameter) {
        return getBoolean(parameter.getId());
    }

    default PatchParameterArgument<String> getString(PatchParameter parameter) {
        return getString(parameter.getId());
    }

    default PatchParameterArgument<List<PatchParameterFile>> getFiles(PatchParameter parameter) {
        return getFiles(parameter.getId());
    }

    List<PatchParameterArgument<Object>> getAll();
}

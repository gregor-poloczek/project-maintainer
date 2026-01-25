package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import java.util.List;

public interface PatchParameterArguments {

    PatchParameterArgument<String> getString(String parameterId);

    default PatchParameterArgument<String> getString(PatchParameter parameter) {
        return getString(parameter.getId());
    }

    PatchParameterArgument<List<PatchParameterFile>> getFiles(String id);
}

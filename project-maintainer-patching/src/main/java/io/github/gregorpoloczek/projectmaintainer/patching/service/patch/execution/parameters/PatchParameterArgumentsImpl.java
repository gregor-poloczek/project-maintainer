package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterArgumentsImpl implements PatchParameterArguments {

    List<PatchParameter> parameters;
    List<PatchParameterArgumentImpl<Object>> arguments;

    @Override
    public PatchParameterArgument<String> getString(String parameterId) {
        PatchParameter patchParameter = getPatchParameter(parameterId);
        return getPatchParameterArgument(patchParameter)
                .map(s -> (PatchParameterArgument<String>) s)
                .orElseGet(() -> new PatchParameterArgumentImpl<>(patchParameter, null));
    }

    @Override
    public PatchParameterArgument<List<PatchParameterFile>> getFiles(String parameterId) {
        PatchParameter patchParameter = getPatchParameter(parameterId);
        return getPatchParameterArgument(patchParameter)
                .map(s -> (PatchParameterArgument<List<PatchParameterFile>>) s)
                .orElseGet(() -> new PatchParameterArgumentImpl<>(patchParameter, null));
    }

    private @org.jspecify.annotations.NonNull PatchParameter getPatchParameter(String parameterId) {
        return parameters.stream().filter(pP -> pP.getId().equals(parameterId)).findFirst().orElseThrow();
    }

    private Optional<? extends PatchParameterArgument<?>> getPatchParameterArgument(PatchParameter parameter) {
        return this.arguments.stream().filter(p -> p.getParameter().getId().equals(parameter.getId())).findFirst();
    }

}

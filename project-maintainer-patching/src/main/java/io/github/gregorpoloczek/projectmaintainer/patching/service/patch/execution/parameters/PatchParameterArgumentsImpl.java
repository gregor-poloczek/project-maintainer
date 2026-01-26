package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterArgumentsImpl implements PatchParameterArguments {

    List<PatchParameter> parameters;
    Collection<PatchParameterArgument<?>> arguments;

    @Override
    public PatchParameterArgument<String> getString(String parameterId) {
        return (PatchParameterArgument<String>) getPatchParameterArgument(parameterId);
    }

    @Override
    public PatchParameterArgument<Integer> getInteger(String parameterId) {
        return (PatchParameterArgument<Integer>) getPatchParameterArgument(parameterId);
    }

    @Override
    public PatchParameterArgument<Boolean> getBoolean(String parameterId) {
        return (PatchParameterArgument<Boolean>) getPatchParameterArgument(parameterId);
    }

    @Override
    public PatchParameterArgument<List<PatchParameterFile>> getFiles(String parameterId) {
        return (PatchParameterArgument<List<PatchParameterFile>>) getPatchParameterArgument(parameterId);
    }

    @Override
    public List<PatchParameterArgument<Object>> getAll() {
        return this.arguments.stream().map(a -> (PatchParameterArgument<Object>) a).toList();
    }

    private PatchParameter getPatchParameter(String parameterId) {
        return parameters.stream().filter(pP -> pP.getId().equals(parameterId)).findFirst().orElseThrow();
    }

    private PatchParameterArgument<?> getPatchParameterArgument(String parameterId) {
        PatchParameter parameter = getPatchParameter(parameterId);
        return this.arguments.stream().filter(p -> p.getParameter().getId().equals(parameter.getId()))
                .findFirst().orElse(new PatchParameterArgumentImpl<>(parameter, null));
    }

}

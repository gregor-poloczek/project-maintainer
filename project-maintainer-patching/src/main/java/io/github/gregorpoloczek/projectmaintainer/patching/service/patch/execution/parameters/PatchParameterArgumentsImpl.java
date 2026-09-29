package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterFile;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterNotFoundException;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterTypeMismatchException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterArgumentsImpl implements PatchParameterArguments {

    List<PatchParameter> parameters;
    Collection<PatchParameterArgument<?>> arguments;

    @Override
    public PatchParameterArgument<String> getString(String parameterId) {
        return getPatchParameterArgument(parameterId, PatchParameterType.STRING, String.class::cast);
    }

    @Override
    public PatchParameterArgument<Integer> getInteger(String parameterId) {
        return getPatchParameterArgument(parameterId, PatchParameterType.INTEGER, Integer.class::cast);
    }

    @Override
    public PatchParameterArgument<Boolean> getBoolean(String parameterId) {
        return getPatchParameterArgument(parameterId, PatchParameterType.BOOLEAN, Boolean.class::cast);
    }

    @Override
    public PatchParameterArgument<List<PatchParameterFile>> getFiles(String parameterId) {
        return getPatchParameterArgument(parameterId, PatchParameterType.FILES,
                value -> ((List<?>) value).stream().map(PatchParameterFile.class::cast).toList());
    }

    @Override
    public List<PatchParameterArgument<Object>> getAll() {
        return this.arguments.stream()
                .<PatchParameterArgument<Object>>map(a -> new PatchParameterArgumentImpl<>(a.getParameter(), a.getValue().orElse(null)))
                .toList();
    }

    private PatchParameter getPatchParameter(String parameterId) {
        return parameters.stream().filter(pP -> pP.getId().equals(parameterId)).findFirst()
                .orElseThrow(() -> new PatchParameterNotFoundException(parameterId));
    }

    /**
     * Looks up the argument for the given parameter and converts its value to the expected type.
     * <p>
     * The declared type of the parameter is checked first, so requesting a parameter as the wrong type always fails,
     * even if no value is present. The converter then performs a checked cast of the value itself.
     */
    private <T> PatchParameterArgument<T> getPatchParameterArgument(String parameterId, PatchParameterType requestedType,
                                                                    Function<Object, T> converter) {
        PatchParameter parameter = getPatchParameter(parameterId);
        if (parameter.getType() != requestedType) {
            throw new PatchParameterTypeMismatchException(parameterId, parameter.getType(), requestedType);
        }
        T value = this.arguments.stream().filter(p -> p.getParameter().getId().equals(parameter.getId()))
                .findFirst()
                .flatMap(PatchParameterArgument::getValue)
                .map(converter)
                .orElse(null);
        return new PatchParameterArgumentImpl<>(parameter, value);
    }

}

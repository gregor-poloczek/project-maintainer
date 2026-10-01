package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters.PatchParameterArgumentImpl;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TestPatchArguments implements Iterable<PatchParameterArgument<?>> {
    @Autowired
    PatchService patchService;

    private final String patchId;
    private final List<PatchParameterArgument<?>> arguments = new ArrayList<>();

    public TestPatchArguments(String patchId) {
        this.patchId = patchId;
    }


    public TestPatchArguments argument(String parameterId, Object value) {
        PatchMetaData patchMetaData = patchService.getPatchMetaData(patchId);
        this.arguments.add(
                new PatchParameterArgumentImpl<>(patchMetaData.requirePatchParameter(parameterId), value));
        return this;
    }

    /**
     * Adds an argument for a parameter that is not declared by the patch itself, e.g. a well known parameter.
     */
    public TestPatchArguments argument(PatchParameter parameter, Object value) {
        this.arguments.add(new PatchParameterArgumentImpl<>(parameter, value));
        return this;
    }

    /**
     * Adds an argument without value for each parameter declared by the patch, for which no argument has been added
     * yet. Meant to be called after all arguments with values have been added, as the patch requires an argument for
     * each of its parameters.
     */
    public TestPatchArguments emptyRemaining() {
        Set<String> present = this.arguments.stream()
                .map(a -> a.getParameter().getId())
                .collect(Collectors.toSet());
        patchService.getPatchMetaData(patchId).getPatchParameters().stream()
                .filter(p -> !present.contains(p.getId()))
                .forEach(p -> this.arguments.add(new PatchParameterArgumentImpl<>(p, null)));
        return this;
    }

    @Override
    public Iterator<PatchParameterArgument<?>> iterator() {
        return arguments.iterator();
    }
}

package io.github.gregorpoloczek.projectmaintainer.patching.common;

import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters.PatchParameterArgumentImpl;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    @Override
    public Iterator<PatchParameterArgument<?>> iterator() {
        return arguments.iterator();
    }
}

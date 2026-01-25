package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.development;

import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BrokenPatch extends AbstractProgrammablePatch {

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::broken-patch")
                .description("Always fails").build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        log.info("Failing on purpose");
        throw new IllegalStateException("Failing on purpose");
    }
}

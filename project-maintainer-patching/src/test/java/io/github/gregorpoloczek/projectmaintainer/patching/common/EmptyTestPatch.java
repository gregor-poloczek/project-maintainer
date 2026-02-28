package io.github.gregorpoloczek.projectmaintainer.patching.common;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.Patch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmptyTestPatch implements Patch {
    public static final String ID = EmptyTestPatch.class.getSimpleName();

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id(ID)
                .description(ID)
                .build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        log.info("Executing {}", this.getMetaData().getId());
    }
}

package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.development;

import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeletingPackageJSONPatch extends AbstractProgrammablePatch {

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::deleting-package-json")
                .description("Deletes package.json").build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        patchingContext.files()
                .findLocation("^package.json$")
                .ifPresent(patchingContext::delete);
    }
}

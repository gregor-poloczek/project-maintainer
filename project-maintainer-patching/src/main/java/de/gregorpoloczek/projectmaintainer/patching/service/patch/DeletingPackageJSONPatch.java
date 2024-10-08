package de.gregorpoloczek.projectmaintainer.patching.service.patch;

import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchContext;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
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

package de.gregorpoloczek.projectmaintainer.patching.service.patch;

import com.google.auto.service.AutoService;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.Patch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchContext;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

@AutoService(Patch.class)
@Slf4j
public class EmptyPatch extends AbstractProgrammablePatch {

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::empty-patch")
                .description("Does nothing").build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        log.info("Executing empty patch");
    }
}

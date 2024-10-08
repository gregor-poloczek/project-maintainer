package de.gregorpoloczek.projectmaintainer.patching.service.patch;

import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchContext;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
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

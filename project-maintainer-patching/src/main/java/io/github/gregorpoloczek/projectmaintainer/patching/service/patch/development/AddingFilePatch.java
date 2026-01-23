package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.development;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddingFilePatch extends AbstractProgrammablePatch {

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::adding-file-patch")
                .description("Creates single file").build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        ProjectFileLocation location = patchingContext.files().get("./project-maintainer.txt");
        patchingContext.create(location, "Maintained by project maintainer. =)");
    }
}

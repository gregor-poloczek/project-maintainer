package de.gregorpoloczek.projectmaintainer.patching.service.patch;

import com.google.auto.service.AutoService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.Patch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchContext;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

@AutoService(Patch.class)
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

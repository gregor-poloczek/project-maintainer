package de.gregorpoloczek.projectmaintainer.patching.service.patch;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchContext;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AddingFilePatch extends AbstractProgrammablePatch {

    private final WorkingCopyService workingCopyService;

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::adding-file-patch")
                .description("Creates single file").build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        ProjectFileLocation location = workingCopyService.require(patchingContext)
                .createLocation("./project-maintainer.txt");
        patchingContext.create(location, "Maintained by project maintainer. =)");
    }
}

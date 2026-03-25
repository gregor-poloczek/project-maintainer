package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.development;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddPrettierPatch extends AbstractProgrammablePatch {

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::add-prettier")
                .description("Adds Prettier").build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        ProjectFileLocation packageJson = patchingContext.files()
                .get("package.json");
        if (!packageJson.exists()) {
            log.info("No package.json found");
            return;
        }
        patchingContext.operations().runProcess(processBuilder -> processBuilder.command("npm", "install", "--save-dev", "prettier"));
    }
}

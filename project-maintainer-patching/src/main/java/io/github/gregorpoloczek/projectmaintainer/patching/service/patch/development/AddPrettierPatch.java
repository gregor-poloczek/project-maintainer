package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.development;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.AbstractProgrammablePatch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
public class AddPrettierPatch extends AbstractProgrammablePatch {

    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::add-prettier")
                .description("Deletes package.json").build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        ProjectFileLocation packageJson = patchingContext.files()
                .get("package.json");
        if (!packageJson.exists()) {
            log.info("No package.json found");
            return;
        }
        try {
            Process process = new ProcessBuilder()
                    .directory(packageJson.getAbsolutePath().getParent().toFile())
                    .command("npm", "install", "--save-dev", "prettier")
                    .inheritIO()
                    .start();
            int status = process.waitFor();
            if (status != 0) {
                throw new IllegalStateException("Failed with status " + status);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}

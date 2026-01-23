package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.development;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.Patch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterFile;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class ParametrizedPatch implements Patch {

    @UtilityClass
    public static class Parameters {
        public static final String COMMIT_MESSAGE = "commit-message";
        public static final String TITLE = "title";
        public static final String FILE = "file";
    }


    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::parametrized-patch")
                .description("Does nothing with parameters")
                .patchParameters(List.of(
                        PatchParameter.builder().id(Parameters.COMMIT_MESSAGE).required(false).type(PatchParameterType.STRING).build(),
                        PatchParameter.builder().id(Parameters.TITLE).required(false).type(PatchParameterType.STRING).build(),
                        PatchParameter.builder().id(Parameters.FILE).required(false).type(PatchParameterType.FILES).build()
                ))
                .build();
    }

    @SneakyThrows({IOException.class})
    @Override
    public void execute(PatchContext patchingContext) {
        PatchParameterArguments arguments = patchingContext.arguments();

        arguments.getString(Parameters.COMMIT_MESSAGE)
                .getValue().filter(StringUtils::isNotBlank)
                .ifPresent(patchingContext::pullRequestCommitMessage);
        arguments.getString(Parameters.TITLE)
                .getValue().filter(StringUtils::isNotBlank)
                .ifPresent(patchingContext::pullRequestTitle);

        List<PatchParameterFile> files = arguments.getFiles(Parameters.FILE).requireValue();

        for (PatchParameterFile file : files) {
            ProjectFileLocation location = patchingContext.files().get("./" + file.getFileName());
            if (!location.exists()) {
                try (InputStream is = file.getInputStream()) {
                    patchingContext.create(location, IOUtils.toString(is, StandardCharsets.UTF_8));
                }
            }
        }
    }
}

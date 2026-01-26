package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.development;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterFile;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.Patch;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Slf4j
public class ParametrizedPatch implements Patch {

    @UtilityClass
    public static class Parameters {
        public static final String COMMIT_MESSAGE = "commit-message";
        public static final String TITLE = "title";
        public static final String FILE = "file";
        public static final String CHECK = "check";
        public static final String COUNT = "count";
        public static final String REQUIRED_TEXT = "required-text";
    }


    @Override
    public PatchMetaData getMetaData() {
        return PatchMetaData.builder()
                .id("project-maintainer::parametrized-patch")
                .description("Does nothing with parameters")
                .patchParameters(List.of(
                        PatchParameter.builder().id(Parameters.COMMIT_MESSAGE).required(false).type(PatchParameterType.STRING).required(false).build(),
                        PatchParameter.builder().id(Parameters.TITLE).required(false).type(PatchParameterType.STRING).build(),
                        PatchParameter.builder().id(Parameters.FILE).required(false).type(PatchParameterType.FILES).build(),
                        PatchParameter.builder().id(Parameters.REQUIRED_TEXT).required(true).type(PatchParameterType.STRING).build(),
                        PatchParameter.builder().id(Parameters.CHECK).required(false).type(PatchParameterType.BOOLEAN).build(),
                        PatchParameter.builder().id(Parameters.COUNT).required(false).type(PatchParameterType.INTEGER).build()
                ))
                .build();
    }

    @Override
    public void execute(PatchContext patchingContext) {
        PatchParameterArguments arguments = patchingContext.arguments();

        arguments.getString(Parameters.COMMIT_MESSAGE)
                .getValue().filter(StringUtils::isNotBlank)
                .ifPresent(patchingContext::pullRequestCommitMessage);
        arguments.getString(Parameters.TITLE)
                .getValue().filter(StringUtils::isNotBlank)
                .ifPresent(patchingContext::pullRequestTitle);

        StringBuilder b = new StringBuilder();
        for (PatchParameterArgument<Object> argument : arguments.getAll()) {
            if (argument.getParameter().getType() == PatchParameterType.FILES && argument.getValue().isPresent()) {
                List<PatchParameterFile> files = argument.getValue().map(a -> (List<PatchParameterFile>) a).get();

                b.append(argument.getParameter().getId()).append(" (%d files)".formatted(files.size())).append(" =\n");
                for (PatchParameterFile file : files) {
                    b.append("* ").append("%s %d %s".formatted(file.getFileName(), file.getSize(), file.getMimetype())).append("\n");
                }
            } else {
                b.append(argument.getParameter().getId()).append(" = ")
                        .append(argument.getValue().map(v -> "[%s] %s".formatted(v.getClass().getName(), v.toString())).orElse("[UNDEFINED]")).append("\n");
            }

        }

        patchingContext.create(patchingContext.files().get("parameters.txt"), b.toString());
    }
}

package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFiles;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFilesImpl;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters.PatchParameterArgumentsImpl;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.NonZeroStatusReturnedException;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.operations.PatchOperations;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArguments;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.UnaryOperator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PatchContextImpl implements PatchContext {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    @NonNull
    PatchMetaData patchMetaData;
    @NonNull
    final Project project;
    @NonNull
    final WorkingCopy workingCopy;
    @NonNull
    final PatchParameterArgumentsImpl arguments;

    String pullRequestCommitMessage;
    String pullRequestTitle;

    @Override
    public FQPN getFQPN() {
        return workingCopy.getFQPN();
    }


    @Override
    public void pullRequestTitle(String title) {
        this.pullRequestTitle = title;
    }

    @Override
    public void pullRequestCommitMessage(String commitMessage) {
        this.pullRequestCommitMessage = commitMessage;
    }

    @Override
    public ProjectFiles files() {
        return new ProjectFilesImpl(this.workingCopy);
    }

    @Override
    public PatchOperations operations() {
        // TODO [Patching] move to PatchService or another place
        return new PatchOperations() {
            public void create(ProjectFileLocation location, String content) {
                try {
                    // TODO [Patching] does the same as update(PFL, String)

                    // TODO [Patching] move to PatchService or another place
                    IOUtils.write(content,
                            new FileOutputStream(location.getAbsolutePath().toFile()), DEFAULT_CHARSET);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            public void update(ProjectFileLocation location, String content) {
                try {
                    // TODO [Patching] move to PatchService or another place
                    IOUtils.write(content,
                            new FileOutputStream(location.getAbsolutePath().toFile()), DEFAULT_CHARSET);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            public void update(ProjectFileLocation location, UnaryOperator<String> transform) {
                try {
                    // TODO [Patching] move to PatchService or another place
                    String before = IOUtils.toString(location.getAbsolutePath().toUri(), DEFAULT_CHARSET);
                    String after = transform.apply(before);
                    this.update(location, after);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            public void delete(ProjectFileLocation location) {
                try {
                    // TODO [Patching] move to PatchService or another place
                    Files.delete(location.getAbsolutePath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void runProcess(UnaryOperator<ProcessBuilder> processBuilderModifier) {
                // TODO [Patching] move to PatchService or another place
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder()
                            .directory(workingCopy.getDirectory())
                            .inheritIO();
                    log.info("Running child process: {}", String.join(" ", processBuilder.command()));
                    Process process = processBuilderModifier.apply(processBuilder).start();
                    int status = process.waitFor();
                    if (status != 0) {
                        throw new NonZeroStatusReturnedException(processBuilder.command(), status);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }

        };
    }

    @Override
    public PatchParameterArguments arguments() {
        return arguments;
    }
}

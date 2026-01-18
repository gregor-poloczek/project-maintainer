package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFiles;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFilesImpl;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchContext;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileCreation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileDeletion;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileOperation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileUpdate;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.IOUtils;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PatchContextImpl implements PatchContext {

    @NonNull
    final Project project;
    @NonNull
    final WorkingCopy workingCopy;

    final List<ProjectFileOperation> operations = new ArrayList<>();

    @Override
    public FQPN getFQPN() {
        return workingCopy.getFQPN();
    }

    @Override
    public void create(ProjectFileLocation location, String content) {
        this.operations.add(new ProjectFileCreation(location, content));
    }

    @Override
    public void update(ProjectFileLocation location, String content) {
        try {
            String before = IOUtils.toString(location.getAbsolutePath().toUri(), StandardCharsets.UTF_8);
            this.operations.add(new ProjectFileUpdate(location, before, content));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void update(ProjectFileLocation location, Function<String, String> transform) {
        try {
            String before = IOUtils.toString(location.getAbsolutePath().toUri(), StandardCharsets.UTF_8);
            String after = transform.apply(before);
            if (!before.equals(after)) {
                this.operations.add(new ProjectFileUpdate(location, before, after));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(ProjectFileLocation location) {
        try {
            String before = IOUtils.toString(location.getAbsolutePath().toUri(), StandardCharsets.UTF_8);
            this.operations.add(new ProjectFileDeletion(location, before));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ProjectFiles files() {
        return new ProjectFilesImpl(this.workingCopy);
    }
}

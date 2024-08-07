package de.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import de.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFiles;
import de.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFilesImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchContext;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileCreation;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileDeletion;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileOperation;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileUpdate;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    String pullRequestTitle;


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

    @Override
    public void pullRequestTitle(String pullRequestTitle) {
        this.pullRequestTitle = pullRequestTitle;
    }

    public Optional<String> getPullRequestTitle() {
        return Optional.ofNullable(pullRequestTitle);
    }
}

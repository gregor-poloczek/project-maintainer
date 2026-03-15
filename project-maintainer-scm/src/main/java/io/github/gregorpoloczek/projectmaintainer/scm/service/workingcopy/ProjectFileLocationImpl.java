package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;

import java.io.File;
import java.nio.file.Path;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ProjectFileLocationImpl implements ProjectFileLocation {

    File file;
    WorkingCopy workingCopy;

    public static ProjectFileLocationImpl of(WorkingCopy workingCopy, File file) {
        return ProjectFileLocationImpl.builder()
                .file(file)
                .workingCopy(workingCopy)
                .build();
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    @EqualsAndHashCode.Include
    @ToString.Include
    @Override
    public Path getRelativePath() {
        return workingCopy.getDirectory().toPath().relativize(file.toPath());
    }

    @Override
    public Path getAbsolutePath() {
        return workingCopy.getDirectory().toPath().resolve(this.getRelativePath());
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return workingCopy.getFQPN();
    }
}

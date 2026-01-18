package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.dependency.Dependency;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnalysisContextImpl implements AnalysisContext, ProjectRelatable {

    private final Project project;
    @Getter
    private final NavigableSet<Label> labels = new TreeSet<>();
    @Getter
    private final WorkingCopy workingCopy;
    @Getter
    private final List<Dependency> dependencies = new ArrayList<>();

    @Override
    public Project getProject() {
        return this.project;
    }

    @Override
    public ProjectFiles files() {
        return new ProjectFilesImpl(this.workingCopy);
    }

    @Override
    public FactsCollector facts() {
        return new FactsCollector(this.labels::add, this.dependencies::add);
    }

    @Override
    public FactsCollector facts(ProjectFileLocation location) {
        return new FactsCollector(
                (Label label) -> this.labels.add(label.withLocation(location)), this.dependencies::add);
    }

    @Override
    public FQPN getFQPN() {
        return this.project.getFQPN();
    }
}

package de.gregorpoloczek.projectmaintainer.analysis.analyzers.common;

import de.gregorpoloczek.projectmaintainer.analysis.Dependency;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import lombok.Getter;
import lombok.NonNull;

public class AnalysisContextImpl implements AnalysisContext, ProjectRelatable {

    private final Project project;

    @Getter
    private final NavigableSet<Label> labels = new TreeSet<>();
    private final WorkingCopy workingCopy;
    @Getter
    private final List<Dependency> dependencies = new ArrayList<>();

    public AnalysisContextImpl(@NonNull final Project project,
            @NonNull final WorkingCopy workingCopy) {
        this.project = project;
        this.workingCopy = workingCopy;
    }

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
    public FactsCollector facts(File file) {
        return new FactsCollector(
                (Label label) -> this.labels.add(label.withLocation(file)), this.dependencies::add);
    }

    @Override
    public FQPN getFQPN() {
        return this.project.getFQPN();
    }
}

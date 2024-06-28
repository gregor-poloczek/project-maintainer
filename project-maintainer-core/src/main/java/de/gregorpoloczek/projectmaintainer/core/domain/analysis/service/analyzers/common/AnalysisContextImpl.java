package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.NavigableSet;
import java.util.TreeSet;
import lombok.Getter;
import lombok.NonNull;

public class AnalysisContextImpl implements AnalysisContext {

    private final Project project;

    @Getter
    private final NavigableSet<Label> labels = new TreeSet<>();
    private final WorkingCopy workingCopy;

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
        return new FactsCollector(this.labels::add);
    }

}

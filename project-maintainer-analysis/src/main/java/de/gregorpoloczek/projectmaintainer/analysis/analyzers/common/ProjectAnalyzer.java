package de.gregorpoloczek.projectmaintainer.analysis.analyzers.common;

import lombok.NonNull;

public interface ProjectAnalyzer {

    void analyze(@NonNull AnalysisContext context);
}

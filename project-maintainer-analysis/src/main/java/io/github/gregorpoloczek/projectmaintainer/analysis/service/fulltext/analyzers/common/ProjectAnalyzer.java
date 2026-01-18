package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common;

import lombok.NonNull;

public interface ProjectAnalyzer {

    void analyze(@NonNull AnalysisContext context);
}

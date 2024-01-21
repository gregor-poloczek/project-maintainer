package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common;

import lombok.NonNull;

public interface ProjectAnalyzer {

  void analyze(@NonNull AnalysisContext context);
}

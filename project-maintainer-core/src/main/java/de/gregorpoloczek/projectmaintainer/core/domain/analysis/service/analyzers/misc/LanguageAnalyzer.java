package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.misc;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.AnalysisContext;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.ProjectFiles;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class LanguageAnalyzer implements ProjectAnalyzer {

  @Override
  public void analyze(final @NonNull AnalysisContext context) {
    final ProjectFiles files = context.files();
    context.facts()
        .when(files.hasAny("\\.tf$")).uses(u -> u.language("terraform"))
        .when(files.hasAny("\\.java$")).uses(u -> u.language("java"))
        .when(files.hasAny("\\.js")).uses(u -> u.language("javascript"))
        .when(files.hasAny("\\.ts$")).uses(u -> u.language("typescript"))
        .when(files.hasAny("\\.[jt]sx$")).uses(u -> u.language("jsx"))
        .when(files.hasAny("\\.cs$")).uses(u -> u.language("c#"))
        .when(files.hasAny("\\.asm$")).uses(u -> u.language("assembly"))
        .when(files.hasAny("\\.c$")).uses(u -> u.language("c"))
        .when(files.hasAny("\\.cpp$")).uses(u -> u.language("c++"));
  }
}

package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.unity;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.AnalysisContext;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectAnalyzer;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFiles;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UnityAnalyzer implements ProjectAnalyzer {

    @Override
    public void analyze(final @NonNull AnalysisContext context) {
        final ProjectFiles files = context.files();
        context.facts().when(files.hasAny("ProjectSettings/[^.]+\\.asset"))
                .uses(u -> u.framework("unity"));
    }

}

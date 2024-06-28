package de.gregorpoloczek.projectmaintainer.analysis.analyzers.unity;

import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.AnalysisContext;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectFiles;
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

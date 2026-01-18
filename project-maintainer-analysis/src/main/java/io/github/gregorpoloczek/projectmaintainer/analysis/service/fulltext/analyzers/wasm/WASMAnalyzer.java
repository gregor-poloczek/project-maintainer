package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.wasm;

import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.AnalysisContext;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectAnalyzer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

@Component
public class WASMAnalyzer implements ProjectAnalyzer {

    @Override
    public void analyze(@NonNull final AnalysisContext context) {
        final SortedSet<File> files = context.files().find("\\.(h|c|hpp|cpp)$");

        for (File file : files) {
            try {
                final String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                if (source.contains("#include <emscripten")) {
                    context.facts().uses(u -> u.framework("wasm"));
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

package de.gregorpoloczek.projectmaintainer.analysis.analyzers.nodejs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.AnalysisContext;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.FactsCollector;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectFiles;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

@Component
public class NodeJSAnalyzer implements ProjectAnalyzer {

    public NodeJSAnalyzer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private final ObjectMapper objectMapper;

    @Override
    public void analyze(final @NonNull AnalysisContext context) {
        final ProjectFiles files = context.files();
        final SortedSet<File> packageJsonFiles = files.find("package\\.json$");

        if (packageJsonFiles.isEmpty()) {
            return;
        }
        for (File file : packageJsonFiles) {
            final FactsCollector facts = context.facts(file);
            PackageJSON packageJSON = this.readPackageJSON(file);

            // collect dependencies
            final SortedMap<String, String> allDependencies = new TreeMap<>();
            packageJSON.name().ifPresent(name -> facts.has(h -> h.label("nodejs", "package.json", "name", name)));

            packageJSON.dependencies().ifPresent(allDependencies::putAll);
            packageJSON.devDependencies().ifPresent(allDependencies::putAll);
            allDependencies.forEach((key, value) -> facts.has(h -> h.dependency("nodejs", key, value)));

            // identify typescript version
            Optional.ofNullable(allDependencies.get("typescript"))
                    .map(this::toRelevantTypeScriptVersion)
                    .ifPresent(
                            v -> facts.uses(u -> u.language("typescript", v))
                    );

            // try to determine nodejs version
            packageJSON.volta().map(v -> v.get("node")).ifPresentOrElse(
                    n -> facts.uses(u -> u.runtime("nodejs", n)),
                    () -> facts.uses(u -> u.runtime("nodejs"))
            );

            final Set<String> packageNames = allDependencies.keySet();
            facts
                    .when(packageNames.stream().anyMatch(p -> p.startsWith("@nestjs")))
                    .uses(u -> u.framework("nestjs"));
            facts.when(packageNames.contains("react")).uses(u -> u.framework("react"));
            facts.when(packageNames.contains("next"))
                    .uses(u -> u.framework("react").framework("next.js"));
            facts.when(packageNames.contains("vue")).uses(u -> u.framework("vue.js"));
            facts.when(packageNames.contains("nuxt"))
                    .uses(u -> u.framework("vue.js").framework("nuxt"));
            facts.when(packageNames.contains("@angular/core")).uses(u -> u.framework("angular"));
            facts.when(packageNames.contains("webpack")).uses(u -> u.framework("webpack"));

            // try to determine yarn version (if used)
            packageJSON.volta().map(v -> v.get("yarn")).ifPresent(
                    n -> facts.uses(u -> u.dependencyManagement("yarn", n))
            );
        }
    }

    private PackageJSON readPackageJSON(final File file) {
        PackageJSON packageJSON;
        try {
            String raw = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            packageJSON = objectMapper.readValue(raw, PackageJSON.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return packageJSON;
    }

    private String toRelevantTypeScriptVersion(String version) {
        final Matcher caret = Pattern.compile("^\\^(?<relevant>\\d).\\d(.\\d)?$").matcher(version);
        final Matcher tilde = Pattern.compile("^\\~(?<relevant>\\d.\\d).\\d$").matcher(version);
        final Matcher exact = Pattern.compile("^(?<relevant>\\d.\\d).\\d$").matcher(version);

        if (caret.matches()) {
            return caret.group("relevant") + ".x";
        } else if (tilde.matches()) {
            return tilde.group("relevant");
        } else if (exact.matches()) {
            return exact.group("relevant");
        }
        return version;
    }
}

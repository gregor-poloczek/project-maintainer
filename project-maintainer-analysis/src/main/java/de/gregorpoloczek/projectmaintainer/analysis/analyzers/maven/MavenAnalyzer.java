package de.gregorpoloczek.projectmaintainer.analysis.analyzers.maven;

import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.AnalysisContext;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.FactsCollector;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.analysis.analyzers.common.ProjectFiles;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.SortedSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MavenAnalyzer implements ProjectAnalyzer {

    @Override
    public void analyze(final @NonNull AnalysisContext context) {
        final ProjectFiles files = context.files();
        final SortedSet<File> poms = files.find("pom\\.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        final FactsCollector facts = context.facts();
        for (File pom : poms) {
            final File cwd = pom.getParentFile();
            final File effectivePom = new File(cwd, "effective-pom.xml");
            try {
                final Process process = new ProcessBuilder("mvn", "help:effective-pom",
                        "-Doutput=effective-pom.xml")
                        .directory(cwd).start();

                final String string = IOUtils.toString(process.getInputStream());
                process.waitFor();
                final int exitCode = process.exitValue();
                if (exitCode != 0) {
                    log.error("Generation of effective pom failed for \"%s\": %s.".formatted(pom, exitCode),
                            string);
                    continue;
                }

                if (!effectivePom.exists()) {
                    throw new IllegalStateException(
                            "File \"%s\" should have been created.".formatted(effectivePom));
                }

                Model model = reader.read(new FileInputStream(effectivePom));
                model.getDependencies().forEach(d -> facts.has(
                        h -> h.dependency("maven", d.getGroupId() + ":" + d.getArtifactId(), d.getVersion())));

                Parent parent = model.getParent();
                if (parent != null) {
                    facts.has(h -> h.dependency("maven", parent.getGroupId() + ":" + parent.getArtifactId(),
                            parent.getVersion()));
                }
                model.getBuild().getPlugins().forEach(plugin -> {
                    facts.has(h -> h.dependency("maven", plugin.getGroupId() + ":" + plugin.getArtifactId(),
                            plugin.getVersion()));
                });

                Optional.ofNullable(model.getProperties().get("maven.compiler.target"))
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(this::convertJavaVersion)
                        .ifPresent(v -> facts.uses(u -> u.language("java", v)));
                Optional.ofNullable(model.getProperties().get("java.version"))
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(this::convertJavaVersion)
                        .ifPresent(v -> facts.uses(u -> u.language("java", v)));
            } catch (IOException | InterruptedException | XmlPullParserException e) {
                log.error("Analysis for \"%s\" failed.".formatted(pom), e);
            } finally {
                effectivePom.delete();
            }
        }

        facts
                .when(files.hasAny("pom\\.xml"))
                .uses(u -> u.dependencyManagement("maven"));
    }

    private String convertJavaVersion(String version) {
        return version.replaceAll("^1\\.(\\d)$", "$1");
    }
}

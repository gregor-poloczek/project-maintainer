package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.maven;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.AnalysisContext;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.ProjectAnalyzer;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common.ProjectFiles;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.FactsCollector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.SortedSet;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Component;

@Component
public class MavenAnalyzer implements ProjectAnalyzer {

  @Override
  public void analyze(final AnalysisContext context) {
    final ProjectFiles files = context.files();
    final SortedSet<File> poms = files.find("pom\\.xml");
    MavenXpp3Reader reader = new MavenXpp3Reader();
    final FactsCollector facts = context.facts();
    for (File pom : poms) {
      try {
        final File cwd = pom.getParentFile();
        final Process process = new ProcessBuilder("mvn", "help:effective-pom",
            "-Doutput=effective-pom.xml")
            .directory(cwd).start();

        final String string = IOUtils.toString(process.getInputStream());
        // process.wait();

        final File effectivePom = new File(cwd, "effective-pom.xml");
        if (!effectivePom.exists()) {
          System.err.println("aaaaaaaa" + " " + effectivePom);
          continue;
        }
        Model model = reader.read(new FileInputStream(effectivePom));
        model.getDependencies().forEach(d -> facts.has(
            h -> h.dependency(d.getGroupId() + ":" + d.getArtifactId(), d.getVersion())));

        Optional.ofNullable(model.getProperties().get("maven.compiler.target"))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .ifPresent(v -> facts.uses(u -> u.language("java", v)));
      } catch (IOException e) {
        e.printStackTrace();
      } catch (XmlPullParserException e) {
        e.printStackTrace();
      }
    }

    facts
        .when(files.hasAny("pom\\.xml"))
        .uses(u -> u.dependencyManagement("maven"));
  }
}

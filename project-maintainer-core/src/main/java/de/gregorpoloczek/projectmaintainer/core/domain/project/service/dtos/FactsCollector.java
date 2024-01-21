package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.VersionedLabel;
import java.util.function.Consumer;

public class FactsCollector {

  private final ProjectImpl project;
  private final boolean keep;

  public FactsCollector(final ProjectImpl project, boolean keep) {
    this.project = project;
    this.keep = keep;
  }

  public FactsCollector(final ProjectImpl project) {
    this(project, true);
  }

  private void addLabel(Label label) {
    if (keep) {
      project.addLabel(label);
    }
  }


  public class Uses {

    public Uses dependencyManagement(String name) {
      addLabel(Label.of("tool", "dependency-management", name));
      return this;
    }

    public Uses dependencyManagement(String name, String version) {
      addLabel(VersionedLabel.of(Label.of("tool", "dependency-management", name), version));
      return this;
    }

    public Uses language(final String language) {
      addLabel(Label.of("lang", language));
      return this;
    }

    public Uses language(final String language, String version) {
      addLabel(VersionedLabel.of(Label.of("lang", language), version));
      return this;
    }

    public Uses runtime(final String runtime, String version) {
      addLabel(VersionedLabel.of(Label.of("runtime", runtime), version));
      return this;
    }

    public Uses runtime(final String runtime) {
      addLabel(Label.of("runtime", runtime));
      return this;
    }
  }

  public class Has {

    public Has dependency(String name, String version) {
      final Label base = Label.of("dependency", name);
      if (version != null) {
        addLabel(VersionedLabel.of(base, version));
      } else {
        addLabel(base);
      }
      return this;
    }
  }

  public FactsCollector when(boolean keep) {
    return new FactsCollector(this.project, keep);
  }

  public FactsCollector uses(Consumer<Uses> usesConsumer) {
    usesConsumer.accept(new Uses());
    return this;
  }

  public FactsCollector has(Consumer<Has> hasConsumer) {
    hasConsumer.accept(new Has());
    return this;
  }
}

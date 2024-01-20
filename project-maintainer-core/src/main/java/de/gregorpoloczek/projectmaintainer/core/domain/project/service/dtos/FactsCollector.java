package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
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

  private void addLabel(String segment, String... segments) {
    if (keep) {
      project.addLabel(Label.of(segment, segments));
    }
  }


  public class Uses {

    public Uses dependencyManagement(String name) {
      addLabel("tool", "dependency-management", name);
      return this;
    }

    public Uses dependencyManagement(String name, String version) {
      addLabel("tool", "dependency-management", name, version);
      return this;
    }

    public Uses language(final String language) {
      addLabel("lang", language);
      return this;
    }

    public Uses language(final String language, String version) {
      addLabel("lang", language, version);
      return this;
    }

    public Uses runtime(final String runtime, String version) {
      addLabel("runtime", runtime, version);
      return this;
    }

    public Uses runtime(final String runtime) {
      addLabel("runtime", runtime);
      return this;
    }
  }

  public class Has {

    public Has dependency(String name, String version) {
      if (version != null) {
        addLabel("dependency", name, version);
      } else {
        addLabel("dependency", name);
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

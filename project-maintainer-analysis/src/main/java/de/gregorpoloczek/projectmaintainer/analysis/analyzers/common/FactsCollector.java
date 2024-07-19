package de.gregorpoloczek.projectmaintainer.analysis.analyzers.common;

import de.gregorpoloczek.projectmaintainer.analysis.Dependency;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.VersionedLabel;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;

public class FactsCollector {

    private final Consumer<Label> labelListener;
    private final Consumer<Dependency> dependencyListener;
    private final boolean keep;

    public FactsCollector(
            @NonNull final Consumer<Label> labelListener,
            @NonNull final Consumer<Dependency> dependencyListener,
            boolean keep) {
        this.labelListener = labelListener;
        this.dependencyListener = dependencyListener;
        this.keep = keep;
    }

    public FactsCollector(
            @NonNull final Consumer<Label> labelListener,
            @NonNull final Consumer<Dependency> dependencyListener) {
        this(labelListener, dependencyListener, true);
    }

    private void addLabel(Label label) {
        if (!keep) {
            return;
        }
        this.labelListener.accept(label);
    }


    public class Uses {

        public Uses dependencyManagement(@NonNull String name) {
            addLabel(Label.of("tool", "dependency-management", name));
            return this;
        }

        public Uses dependencyManagement(@NonNull String name, @NonNull String version) {
            addLabel(VersionedLabel.of(Label.of("tool", "dependency-management", name), version));
            return this;
        }

        public Uses language(@NonNull final String language) {
            addLabel(Label.of("lang", language));
            return this;
        }

        public Uses language(@NonNull final String language, @NonNull String version) {
            addLabel(VersionedLabel.of(Label.of("lang", language), version));
            return this;
        }

        public Uses runtime(@NonNull final String runtime, @NonNull String version) {
            addLabel(VersionedLabel.of(Label.of("runtime", runtime), version));
            return this;
        }

        public Uses runtime(@NonNull final String runtime) {
            addLabel(Label.of("runtime", runtime));
            return this;
        }

        public Uses framework(final String name) {
            addLabel(Label.of("framework", name));
            return this;
        }
    }

    public class Has {

        public Has dependency(String management, @NonNull String name, String version) {
            final Label base = Label.of("dep", name);
            if (version != null) {
                addLabel(VersionedLabel.of(base, version));
            } else {
                addLabel(base);
            }
            addDependency(Dependency.builder()
                    .type(management)
                    .name(name)
                    .version(Optional.ofNullable(version))
                    .build());
            return this;
        }

        public void label(String segment, String... moreSegments) {
            addLabel(Label.of(segment, moreSegments));
        }
    }

    private void addDependency(Dependency dependency) {
        this.dependencyListener.accept(dependency);
    }

    public FactsCollector when(boolean keep) {
        return new FactsCollector(this.labelListener, this.dependencyListener, keep);
    }

    public FactsCollector uses(@NonNull Consumer<Uses> usesConsumer) {
        usesConsumer.accept(new Uses());
        return this;
    }

    public FactsCollector has(@NonNull Consumer<Has> hasConsumer) {
        hasConsumer.accept(new Has());
        return this;
    }
}

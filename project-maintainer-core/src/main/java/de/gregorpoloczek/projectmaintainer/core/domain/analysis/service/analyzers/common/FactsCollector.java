package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.VersionedLabel;
import java.util.function.Consumer;
import lombok.NonNull;

public class FactsCollector {

    private final Consumer<Label> labelListener;
    private final boolean keep;

    public FactsCollector(@NonNull final Consumer<Label> labelListener, boolean keep) {
        this.labelListener = labelListener;
        this.keep = keep;
    }

    public FactsCollector(@NonNull final Consumer<Label> labelListener) {
        this(labelListener, true);
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

        public Has dependency(@NonNull String name, String version) {
            final Label base = Label.of("dep", name);
            if (version != null) {
                addLabel(VersionedLabel.of(base, version));
            } else {
                addLabel(base);
            }
            return this;
        }
    }

    public FactsCollector when(boolean keep) {
        return new FactsCollector(this.labelListener, keep);
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

package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractComposable<S extends AbstractComposable<S>> implements Composable<S> {

    private final Map<Class<?>, Object> traits = new HashMap<>();

    public <C> Optional<C> getTrait(Class<C> traitClass) {
        return Optional.ofNullable(this.traits.get(traitClass))
                .map(traitClass::cast);
    }

    public <C> C requireTrait(Class<C> traitClass) {
        return this.getTrait(traitClass)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Composable (%s) does not have component of type \"%s\".".formatted(
                                this.getClass().getSimpleName(), traitClass.getName())));
    }

    public <C, I extends C> S addTrait(Class<C> traitClass, I trait) {
        this.traits.put(traitClass, trait);
        return (S) this;
    }

    public <C, I extends C> S replaceTrait(Class<C> traitClass, Function<I, I> replacer) {
        I component = (I) traitClass.cast(this.traits.get(traitClass));

        I newComponent = replacer.apply(component);
        if (newComponent == null) {
            throw new IllegalStateException("Component replacement may not be null");
        }

        this.traits.put(traitClass, newComponent);
        return (S) this;
    }

    public <C> void removeTrait(Class<C> traitClass) {
        this.traits.remove(traitClass);
    }
}

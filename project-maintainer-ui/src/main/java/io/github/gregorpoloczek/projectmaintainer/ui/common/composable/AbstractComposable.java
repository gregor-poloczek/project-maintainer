package io.github.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public abstract class AbstractComposable<K, S extends AbstractComposable<K, S>> implements Composable<K, S> {


    private final Map<Class<?>, Object> traits = new HashMap<>();

    private final List<ChangeListener<K, S>> changeListeners = new ArrayList<>();

    @Override
    public void addChangeListener(ChangeListener<K, S> changeListener) {
        this.changeListeners.add(changeListener);
    }

    @Override
    public void removeChangeListener(ChangeListener<K, S> changeListener) {
        this.changeListeners.remove(changeListener);
    }

    @Override
    public <C> Optional<C> getTrait(Class<C> traitClass) {
        return Optional.ofNullable(this.traits.get(traitClass))
                .map(traitClass::cast);
    }

    @Override
    public <C> C requireTrait(Class<C> traitClass) {
        return this.getTrait(traitClass)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Composable (%s) does not have component of type \"%s\".".formatted(
                                this.getClass().getSimpleName(), traitClass.getName())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C, T extends C> S addTrait(Class<C> traitClass, T trait) {
        this.traits.put(traitClass, trait);
        triggerUpdateListeners();
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C, T extends C> S replaceTrait(Class<C> traitClass, UnaryOperator<T> replacer) {
        T component = (T) traitClass.cast(this.traits.get(traitClass));

        T newComponent = replacer.apply(component);
        if (newComponent == null) {
            throw new IllegalStateException("Component replacement for %s may not be null".formatted(traitClass));
        }

        this.traits.put(traitClass, newComponent);

        triggerUpdateListeners();
        return (S) this;
    }

    private void triggerUpdateListeners() {
        this.changeListeners.forEach(l -> l.onChange((S) this));
    }

    @Override
    public <C> void removeTrait(Class<C> traitClass) {
        this.traits.remove(traitClass);
        triggerUpdateListeners();
    }
}

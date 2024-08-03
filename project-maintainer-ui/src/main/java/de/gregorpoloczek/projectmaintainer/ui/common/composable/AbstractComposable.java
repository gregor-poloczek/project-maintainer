package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AbstractComposable<S extends AbstractComposable<S>> implements Composable<S> {

    private Map<Class<?>, Object> components = new HashMap<>();

    public <C> Optional<C> getComponent(Class<C> componentClass) {
        return Optional.ofNullable(this.components.get(componentClass))
                .map(componentClass::cast);
    }

    public <C> C requireComponent(Class<C> componentClass) {
        return this.getComponent(componentClass)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Composable (%s) does not have component of type \"%s\".".formatted(
                                this.getClass().getSimpleName(), componentClass.getName())));
    }

    public <C, I extends C> S addComponent(Class<C> componentClass, I component) {
        this.components.put(componentClass, component);
        return (S) this;
    }

    public <C> void removeComponent(Class<C> componentClass) {
        this.components.remove(componentClass);
    }
}

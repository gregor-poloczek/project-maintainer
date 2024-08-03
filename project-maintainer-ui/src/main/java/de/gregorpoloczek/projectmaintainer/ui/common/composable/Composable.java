package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.Optional;
import java.util.function.Function;

public interface Composable<S extends Composable<S>> {

    <C> Optional<C> getComponent(Class<C> componentClass);

    <C> C requireComponent(Class<C> componentClass);

    <C, I extends C> S addComponent(Class<C> componentClass, I component);

    <C, I extends C> S replaceComponent(Class<C> componentClass, Function<I, I> replacer);

    <C> void removeComponent(Class<C> componentClass);
}

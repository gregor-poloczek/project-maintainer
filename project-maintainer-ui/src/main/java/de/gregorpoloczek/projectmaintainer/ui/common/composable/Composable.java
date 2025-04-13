package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.Optional;
import java.util.function.UnaryOperator;

public interface Composable<S extends Composable<S>> {

    <C> Optional<C> getTrait(Class<C> traitClass);

    <C> C requireTrait(Class<C> traitClass);

    <T, I extends T> S addTrait(Class<T> traitClass, I trait);

    <T, I extends T> S replaceTrait(Class<T> componentClass, UnaryOperator<I> replacer);

    <T> void removeTrait(Class<T> traitClass);
}

package io.github.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.Optional;
import java.util.function.UnaryOperator;

public interface Composable<K, S extends Composable<K, S>> {


    @FunctionalInterface
    interface ChangeListener<K2, S2 extends Composable<K2, S2>> {

        void onChange(S2 composable);
    }

    K getKey();

    void addChangeListener(ChangeListener<K, S> changeListener);

    void removeChangeListener(ChangeListener<K, S> changeListener);

    <C> Optional<C> getTrait(Class<C> traitClass);

    <C> C requireTrait(Class<C> traitClass);

    <C, I extends C> S addTrait(Class<C> traitClass, I trait);

    <C, T extends C> S replaceTrait(Class<C> componentClass, UnaryOperator<T> replacer);

    <C> void removeTrait(Class<C> traitClass);
}

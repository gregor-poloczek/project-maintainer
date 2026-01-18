package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter;

import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;

@FunctionalInterface
public interface ComposableFilterCriterion<K, T extends Composable<K, T>> {

    boolean matches(T composable);
}

package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter;

import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;

@FunctionalInterface
public interface ComposableFilterCriterion<T extends Composable<?, T>> {

    boolean matches(T composable);
}

package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter;

import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComposableFilterCriterionHandle<K, T extends Composable<K, T>> {

    ComposableFilterSearch<K, T> composableFilterSearch;

    @Getter(AccessLevel.PACKAGE)
    ComposableFilterCriterion<K, T> criterion;

    public void refresh() {
        this.composableFilterSearch.refresh(this);
    }

    public void remove() {
        this.composableFilterSearch.release(this);
    }
}

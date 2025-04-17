package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter;

import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComposableFilterCriterionHandle<T extends Composable<?, T>> {

    int index;
    ComposableFilterSearch<T> composableFilterSearch;

    public void refresh() {
        this.composableFilterSearch.refresh(index);
    }

    public void remove() {
        this.composableFilterSearch.release(index);
    }
}

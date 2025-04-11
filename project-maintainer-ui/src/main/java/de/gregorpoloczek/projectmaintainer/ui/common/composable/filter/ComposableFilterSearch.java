package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter;

import com.vaadin.flow.data.provider.ListDataProvider;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComposableFilterSearch<T extends Composable<T>> {

    private final List<ComposableFilterCriterion<T>> criteria = new ArrayList<>();

    @FunctionalInterface
    public interface Refreshable {

        void refresh();
    }

    private final Refreshable refreshale;

    public ComposableFilterSearch(ListDataProvider<T> listDataProvider) {
        this.refreshale = listDataProvider::refreshAll;
        listDataProvider.setFilter(this::matches);
    }

    public ComposableFilterSearch<T> add(ComposableFilterCriterion<T> criterion) {
        this.criteria.add(criterion);
        return this;
    }

    public boolean matches(T composable) {
        return this.criteria.stream().allMatch(c -> c.matches(composable));
    }

    public void refresh() {
        Optional.ofNullable(refreshale).ifPresent(Refreshable::refresh);
    }
}

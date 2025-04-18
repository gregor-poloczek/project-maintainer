package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter;

import com.vaadin.flow.data.provider.ListDataProvider;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ComposableFilterSearch<T extends Composable<?, T>> {


    public enum FilterResult {
        HIT {
            @Override
            boolean isMatch() {
                return true;
            }
        },
        MISS {
            @Override
            boolean isMatch() {
                return false;
            }
        },
        DIRTY {
            @Override
            boolean isMatch() {
                throw new IllegalStateException();
            }
        };

        static FilterResult of(boolean matches) {
            return matches ? FilterResult.HIT : FilterResult.MISS;
        }

        abstract boolean isMatch();
    }

    private final List<ComposableFilterCriterion<T>> criteria = new ArrayList<>();
    private final Map<Object, FilterResult[]> resultsCache = new HashMap<>();

    @FunctionalInterface
    public interface Refreshable {

        void refresh();
    }

    private final Refreshable refreshable;

    public ComposableFilterSearch(ListDataProvider<T> listDataProvider) {
        this.refreshable = listDataProvider::refreshAll;
        listDataProvider.setFilter(this::matches);
    }

    public ComposableFilterCriterionHandle<T> add(ComposableFilterCriterion<T> criterion) {
        this.criteria.add(criterion);
        this.resultsCache.clear();
        return new ComposableFilterCriterionHandle<>(this, criterion);
    }

    public boolean matches(T composable) {
        // TODO use key of composable
        FilterResult[] results = this.resultsCache.computeIfAbsent(composable.getKey(),
                (_) -> {
                    FilterResult[] init = new FilterResult[this.criteria.size()];
                    Arrays.fill(init, FilterResult.DIRTY);
                    return init;
                });
        boolean result = true;
        for (int i = 0; i < results.length; i++) {
            if (results[i] == FilterResult.DIRTY) {
                results[i] = FilterResult.of(this.criteria.get(i).matches(composable));
            }
            result &= results[i].isMatch();
        }
        return result;
    }

    public void refresh() {
        Optional.ofNullable(refreshable).ifPresent(Refreshable::refresh);
    }


    void refresh(ComposableFilterCriterionHandle<T> handle) {
        int index = this.criteria.indexOf(handle.getCriterion());

        // mark all as dirty
        this.resultsCache.values().forEach(r -> r[index] = FilterResult.DIRTY);

        this.refresh();
    }

    public void release(ComposableFilterCriterionHandle<T> handle) {
        this.criteria.remove(handle.getCriterion());
        this.resultsCache.clear();
    }

}

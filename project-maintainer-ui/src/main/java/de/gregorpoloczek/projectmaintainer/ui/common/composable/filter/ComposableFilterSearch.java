package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter;

import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComposableFilterSearch<K, T extends Composable<K, T>> {


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

    private final List<ComposableFilterCriterion<K, T>> criteria = new ArrayList<>();
    private final Map<K, FilterResult[]> resultsCache = new HashMap<>();
    private final ComposableHolder<K, T> composables = new ComposableHolder<>();

    @FunctionalInterface
    public interface Refreshable<T2> {

        void setFilter(SerializablePredicate<T2> filter);
    }

    private final Refreshable<T> refreshable;

    public ComposableFilterSearch(ListDataProvider<T> listDataProvider) {
        this.refreshable = listDataProvider::setFilter;
        this.refreshable.setFilter(this::matches);
    }

    public ComposableFilterCriterionHandle<K, T> add(ComposableFilterCriterion<K, T> criterion) {
        this.criteria.add(criterion);
        this.resultsCache.clear();
        return new ComposableFilterCriterionHandle<>(this, criterion);
    }

    void onComposableChange(T composable) {
        FilterResult[] a = this.resultsCache.get(composable.getKey());
        Arrays.fill(a, FilterResult.DIRTY);
    }

    public boolean matches(T composable) {
        if (!composables.contains(composable)) {
            // TODO unklar, ob man den listener wirklich wegräumen muss
            composable.addChangeListener(this::onComposableChange);
            composables.add(composable);
        }

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
        // TODO darf nur gemacht werden, wenn sich tatsächlich etwas ändert, sonst
        //  wird es zu teuer.
        this.refreshable.setFilter(this::matches);
    }


    void refresh(ComposableFilterCriterionHandle<K, T> handle) {
        int index = this.criteria.indexOf(handle.getCriterion());

        // mark all as dirty
        this.resultsCache.values().forEach(r -> r[index] = FilterResult.DIRTY);

        this.refresh();
    }

    public void release(ComposableFilterCriterionHandle<K, T> handle) {
        this.criteria.remove(handle.getCriterion());
        this.resultsCache.clear();
    }

}

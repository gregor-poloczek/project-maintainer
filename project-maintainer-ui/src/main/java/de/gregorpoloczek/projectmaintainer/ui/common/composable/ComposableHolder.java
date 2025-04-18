package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class ComposableHolder<K, T extends Composable<K, T>> {

    private static final ComposableHolder<?, ?> EMPTY = new ComposableHolder<>();

    public static <K2, T2 extends Composable<K2, T2>> Collector<T2, ComposableHolder<K2, T2>, ComposableHolder<K2, T2>> toComposableHolder() {
        return new ComposableHolderCollector<>();
    }

    final Map<K, T> composables = new LinkedHashMap<>();

    public static <K2, T2 extends Composable<K2, T2>> ComposableHolder<K2, T2> of(Collection<T2> c) {
        ComposableHolder<K2, T2> result = new ComposableHolder<>();
        c.forEach(composable -> result.composables.put(composable.getKey(), composable));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <K2, T2 extends Composable<K2, T2>> ComposableHolder<K2, T2> emptyHolder() {
        return (ComposableHolder<K2, T2>) EMPTY;
    }


    public T get(K key) {
        return this.composables.get(key);
    }

    public List<T> getAll() {
        return new ArrayList<>(this.composables.values());
    }

    public Stream<T> stream() {
        return getAll().stream();
    }
}

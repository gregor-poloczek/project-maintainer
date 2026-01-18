package io.github.gregorpoloczek.projectmaintainer.ui.common.composable;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

class ComposableHolderCollector<K, T extends Composable<K, T>> implements
        Collector<T, ComposableHolder<K, T>, ComposableHolder<K, T>> {

    @Override
    public Supplier<ComposableHolder<K, T>> supplier() {
        return ComposableHolder::new;
    }

    @Override
    public BiConsumer<ComposableHolder<K, T>, T> accumulator() {
        return (a, b) -> a.composables.put(b.getKey(), b);
    }

    @Override
    public BinaryOperator<ComposableHolder<K, T>> combiner() {
        return (a, b) -> {
            a.composables.putAll(b.composables);
            return a;
        };
    }

    @Override
    public Function<ComposableHolder<K, T>, ComposableHolder<K, T>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.IDENTITY_FINISH);
    }
}

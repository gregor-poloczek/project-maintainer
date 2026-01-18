package io.github.gregorpoloczek.projectmaintainer.core.common.facets;

import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public interface HasFacets {
    <C> Optional<C> getFacet(Class<C> facetClass);

    default @NotNull <C> C requireFacet(Class<C> facetClass) {
        return getFacet(facetClass).orElseThrow(() -> new IllegalStateException("No such facet " + facetClass.getName()));
    }

    default boolean hasFacet(Class<?> traitClass) {
        return getFacet(traitClass).isPresent();
    }
}

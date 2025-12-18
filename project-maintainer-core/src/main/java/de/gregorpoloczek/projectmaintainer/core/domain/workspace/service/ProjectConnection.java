package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import de.gregorpoloczek.projectmaintainer.core.common.facets.HasFacets;

import java.util.Optional;

public interface ProjectConnection extends HasFacets {
    String getId();

    String getType();

    @Override
    default <C> Optional<C> getFacet(Class<C> facetClass) {
        return Optional.empty();
    }
}

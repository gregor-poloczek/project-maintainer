package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service;

import io.github.gregorpoloczek.projectmaintainer.core.common.facets.HasFacets;

import java.net.URI;
import java.util.function.Supplier;

public interface Project extends ProjectRelatable, HasFacets {

    String getWorkspaceId();

    String getConnectionId();

    URI getURI();

    ProjectMetaData getMetaData();

    <T> T withReadLock(Supplier<T> operation);

    <T> T withWriteLock(Supplier<T> operation);
}

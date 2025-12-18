package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectImpl implements Project {

    private final String workspaceId;
    private final String connectionId;
    private final ProjectMetaData metaData;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<Class<?>, Object> facets = new LinkedHashMap<>();

    public ProjectImpl(String workspaceId, String connectionId, ProjectMetaData metaData) {
        this.workspaceId = workspaceId;
        this.connectionId = connectionId;
        this.metaData = metaData;
    }

    public <C, F extends C> void addFacet(Class<C> facetClass, F facet) {
        this.facets.put(facetClass, facet);
    }

    @Override
    public <C> Optional<C> getFacet(Class<C> facetClass) {
        return Optional.ofNullable(this.facets.get(facetClass)).map(facetClass::cast);
    }

    public URI getURI() {
        return this.metaData.getURI();
    }

    @Override
    public <T> T withReadLock(final Supplier<T> operation) {
        lock.readLock().lock();
        try {
            return operation.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T> T withWriteLock(final Supplier<T> operation) {
        lock.writeLock().lock();
        try {
            return operation.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.metaData.getFQPN();
    }
}

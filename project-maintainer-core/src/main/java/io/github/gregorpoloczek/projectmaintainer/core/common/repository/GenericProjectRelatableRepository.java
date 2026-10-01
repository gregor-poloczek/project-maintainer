package io.github.gregorpoloczek.projectmaintainer.core.common.repository;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class GenericProjectRelatableRepository<T> implements ProjectRelatableRepository<T> {

    private final Map<FQPN, T> data = Collections.synchronizedSortedMap(new TreeMap<>());

    @Override
    public Optional<T> find(ProjectRelatable projectRelatable) {
        return Optional.ofNullable(data.get(projectRelatable.getFQPN()));
    }

    @Override
    public T require(ProjectRelatable projectRelatable) {
        return this.find(projectRelatable).orElseThrow(() -> this.createNotFoundException(projectRelatable));
    }

    /**
     * Creates the exception thrown by {@link #require(ProjectRelatable)} if no data exists for the given project.
     * Subclasses may override this method to throw a dedicated exception.
     *
     * @param projectRelatable the project no data was found for
     * @return the exception to throw, never {@code null}
     */
    protected RuntimeException createNotFoundException(ProjectRelatable projectRelatable) {
        return new IllegalStateException("Unable to find data for %s".formatted(projectRelatable.getFQPN()));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(data.values());
    }

    @Override
    public void save(ProjectRelatable projectRelatable, T data) {
        this.data.put(projectRelatable.getFQPN(), data);
    }

    @Override
    public boolean delete(ProjectRelatable projectRelatable) {
        return this.data.remove(projectRelatable.getFQPN()) != null;
    }

    @Override
    public void deleteAll() {
        this.data.clear();
    }
}

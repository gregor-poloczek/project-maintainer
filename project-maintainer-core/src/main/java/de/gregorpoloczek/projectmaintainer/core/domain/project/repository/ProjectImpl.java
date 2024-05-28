package de.gregorpoloczek.projectmaintainer.core.domain.project.repository;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.net.URI;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;

@Getter
@Slf4j
public class ProjectImpl implements Project {

    private final ProjectMetaData metaData;
    private final CredentialsProvider credentialsProvider;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private NavigableSet<Label> labels = new TreeSet<>();

    public ProjectImpl(ProjectMetaData metaData, final CredentialsProvider credentialsProvider) {
        this.metaData = metaData;
        this.credentialsProvider = credentialsProvider;
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

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final ProjectImpl that = (ProjectImpl) object;

        return new EqualsBuilder().append(this.getMetaData().getFQPN(), that.getMetaData().getFQPN()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(this.getMetaData().getFQPN()).toHashCode();
    }

    public void setLabels(final Collection<Label> labels) {
        this.labels.clear();
        this.labels.addAll(labels);
    }

    @Override
    public SortedSet<Label> getLabels() {
        return this.labels;
    }

}

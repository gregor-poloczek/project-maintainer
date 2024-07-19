package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.net.URI;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.CredentialsProvider;

@Getter
@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectImpl implements Project {

    private final ProjectMetaData metaData;
    private final CredentialsProvider credentialsProvider;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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

    @EqualsAndHashCode.Include
    @Override
    public FQPN getFQPN() {
        return this.metaData.getFQPN();
    }
}

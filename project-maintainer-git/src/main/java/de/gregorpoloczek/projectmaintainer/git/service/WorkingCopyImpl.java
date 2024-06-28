package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.jgit.transport.CredentialsProvider;

@Getter
public class WorkingCopyImpl implements WorkingCopy {

    private final File directory;
    private final URI uri;
    private final CredentialsProvider credentialsProvider;
    @Getter(AccessLevel.NONE)
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private final FQPN fqpn;
    private final Optional<Commit> latestCommit;

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public FQPN getFQPN() {
        return fqpn;
    }


    public WorkingCopyImpl(
            @NonNull final FQPN fqpn,
            @NonNull final URI uri,
            @NonNull final File directory,
            final Commit latestCommit,
            CredentialsProvider credentialsProvider
    ) {
        this.directory = directory;
        this.uri = uri;
        this.fqpn = fqpn;
        this.latestCommit = Optional.ofNullable(latestCommit);
        this.credentialsProvider = credentialsProvider;
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

}

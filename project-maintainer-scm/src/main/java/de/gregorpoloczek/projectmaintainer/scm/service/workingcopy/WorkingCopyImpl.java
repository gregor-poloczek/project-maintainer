package de.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.scm.service.git.Commit;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.eclipse.jgit.transport.CredentialsProvider;

@Getter
@Builder
public class WorkingCopyImpl implements WorkingCopy {

    private final String currentBranch;
    private final FQPN fqpn;
    private final File directory;
    private final URI uri;
    private final CredentialsProvider credentialsProvider;
    private final Commit latestCommit;
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public Optional<Commit> getLatestCommit() {
        return Optional.ofNullable(latestCommit);
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public FQPN getFQPN() {
        return fqpn;
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
    public void withReadLock(final Runnable operation) {
        this.withReadLock(((Supplier<Void>) (() -> {
            operation.run();
            return null;
        })));
    }

    @Override
    public void withWriteLock(final Runnable operation) {
        this.withWriteLock(((Supplier<Void>) (() -> {
            operation.run();
            return null;
        })));
    }
}

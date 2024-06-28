package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface WorkingCopy {

    File getDirectory();

    Optional<Commit> getLatestCommit();

    FQPN getFQPN();

    URI getURI();

    <T> T withReadLock(Supplier<T> operation);

    <T> T withWriteLock(Supplier<T> operation);

    CredentialsProvider getCredentialsProvider();
}

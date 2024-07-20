package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface WorkingCopy extends ProjectRelatable {

    FQPN getFQPN();

    File getDirectory();

    Optional<Commit> getLatestCommit();

    String getCurrentBranch();

    URI getURI();

    <T> T withReadLock(Supplier<T> operation);

    <T> T withWriteLock(Supplier<T> operation);

    CredentialsProvider getCredentialsProvider();
}

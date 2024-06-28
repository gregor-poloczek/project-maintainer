package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.net.URI;
import java.util.function.Supplier;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface Project {

    URI getURI();

    ProjectMetaData getMetaData();

    <T> T withReadLock(Supplier<T> operation);

    <T> T withWriteLock(Supplier<T> operation);

    CredentialsProvider getCredentialsProvider();
}

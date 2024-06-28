package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import java.util.function.Supplier;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface Project {

    ProjectMetaData getMetaData();

    <T> T withReadLock(Supplier<T> operation);

    <T> T withWriteLock(Supplier<T> operation);

    CredentialsProvider getCredentialsProvider();
}

package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
import java.util.SortedSet;
import java.util.function.Supplier;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface Project {

    @Deprecated
    boolean isCloned();

    ProjectMetaData getMetaData();

    @Deprecated
    Commit getLatestCommit();

    SortedSet<Label> getLabels();

    <T> T withReadLock(Supplier<T> operation);

    <T> T withWriteLock(Supplier<T> operation);

    CredentialsProvider getCredentialsProvider();
}

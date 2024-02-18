package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import java.net.URI;
import lombok.NonNull;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface GitProjectResolver {

  CredentialsProvider getCredentialsProvider(@NonNull WorkingCopy workingCopy);

  boolean supports(@NonNull URI uri);
}

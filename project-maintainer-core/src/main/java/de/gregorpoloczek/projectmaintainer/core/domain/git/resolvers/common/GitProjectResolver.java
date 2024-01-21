package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.net.URI;
import lombok.NonNull;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface GitProjectResolver {

  CredentialsProvider getCredentialsProvider(@NonNull URI uri);

  ProjectMetaData getProjectMetaData(@NonNull URI uri);

  boolean supports(@NonNull URI uri);
}

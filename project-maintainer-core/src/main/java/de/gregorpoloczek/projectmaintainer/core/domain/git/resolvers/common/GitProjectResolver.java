package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.net.URI;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface GitProjectResolver {

  CredentialsProvider getCredentialsProvider(URI uri);

  ProjectMetaData getProjectMetaData(URI uri);

  boolean supports(URI uri);
}

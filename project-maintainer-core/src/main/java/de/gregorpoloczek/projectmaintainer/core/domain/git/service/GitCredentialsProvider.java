package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import java.net.URI;
import org.eclipse.jgit.transport.CredentialsProvider;

// TODO umbenennen
public interface GitCredentialsProvider {

  CredentialsProvider getCredentialsProvider();

  ProjectMetaData getProjectMetaData(URI uri);

  boolean supports(URI uri);
}

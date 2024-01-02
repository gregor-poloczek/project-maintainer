package de.gregorpoloczek.projectmaintainer.core.git.common;

import java.net.URI;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface GitCredentialsProvider {

  CredentialsProvider getCredentialsProvider();

  boolean supports(URI uri);
}

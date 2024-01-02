package de.gregorpoloczek.projectmaintainer.core.git.common;

import java.net.URI;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface GitCredentialsProvider {

  CredentialsProvider getCredentialsProvider();

  String getFQPN(URI uri);

  boolean supports(URI uri);
}

package de.gregorpoloczek.projectmaintainer.core.git.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.net.URI;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface GitCredentialsProvider {

  CredentialsProvider getCredentialsProvider();

  FQPN getFQPN(URI uri);

  boolean supports(URI uri);
}

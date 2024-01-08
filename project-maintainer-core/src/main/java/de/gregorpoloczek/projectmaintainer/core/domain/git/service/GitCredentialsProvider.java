package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface GitCredentialsProvider {

  CredentialsProvider getCredentialsProvider();

  FQPN getFQPN(URI uri);

  boolean supports(URI uri);
}

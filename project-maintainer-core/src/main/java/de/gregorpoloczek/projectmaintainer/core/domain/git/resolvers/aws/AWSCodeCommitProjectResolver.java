package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.aws;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.GitProjectResolver;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import java.net.URI;
import java.util.regex.Pattern;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AWSCodeCommitProjectResolver implements GitProjectResolver {

  private static final Pattern AWS_CODE_COMMIT = Pattern.compile(
      "^\\Qhttps://git-codecommit.\\E(?<region>[^.]+)\\Q.amazonaws.com\\E\\/v1\\/repos\\/(?<repository>.+)$");

  @Value("file:./.credentials/aws-codecommit.properties")
  private Resource credentials;

  private final ConversionService conversionService;

  public AWSCodeCommitProjectResolver(final ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public CredentialsProvider getCredentialsProvider(WorkingCopy workingCopy) {
    final AWSCodeCommitCredentials credentials =
        workingCopy.getGitCredentials(AWSCodeCommitCredentials.class);

    return new UsernamePasswordCredentialsProvider(
        credentials.username(),
        credentials.password());
  }

  @Override
  public boolean supports(final URI uri) {
    return uri.toString().startsWith("https://git-codecommi");
  }

}

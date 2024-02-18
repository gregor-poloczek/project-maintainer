package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.aws;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.GitProjectResolver;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
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

  @Override
  public ProjectMetaData getProjectMetaData(final URI uri) {
    final String account;

    try {
      // TODO determine right credentials for uri
      final Properties credentials = conversionService.convert(
          this.credentials.getContentAsString(StandardCharsets.UTF_8),
          Properties.class);
      String username = credentials.getProperty("username");
      final Matcher matcher = Pattern.compile("^(?<username>.+?)-at-(?<account>\\d+)$")
          .matcher(username);

      if (!matcher.matches()) {
        throw new IllegalStateException("Cannot determined account from " + username);
      }

      account = matcher.group("account");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    final Matcher matcher = AWS_CODE_COMMIT.matcher(uri.toString());
    if (matcher.matches()) {
      final String region = matcher.group("region");
      final String repository = matcher.group("repository");
      return ProjectMetaData.builder()
          .owner(account)
          .name(repository)
          .fqpn("aws-codecommit", account, region, repository)
          .uri(uri).build();
    }
    throw new IllegalStateException(uri.toString());
  }

}

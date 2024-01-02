package de.gregorpoloczek.projectmaintainer.core.git.github;

import de.gregorpoloczek.projectmaintainer.core.git.common.GitCredentialsProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AWSCodeCommitCredentialsProvider implements GitCredentialsProvider {

  @Value("file:./.credentials/aws-codecommit.properties")
  private Resource credentials;

  private final ConversionService conversionService;

  public AWSCodeCommitCredentialsProvider(final ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public CredentialsProvider getCredentialsProvider() {
    try {
      final Properties credentials = conversionService.convert(
          this.credentials.getContentAsString(StandardCharsets.UTF_8),
          Properties.class);

      return new UsernamePasswordCredentialsProvider(
          credentials.getProperty("username"),
          credentials.getProperty("password"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean supports(final URI uri) {
    return uri.toString().startsWith("https://git-codecommi");
  }
}

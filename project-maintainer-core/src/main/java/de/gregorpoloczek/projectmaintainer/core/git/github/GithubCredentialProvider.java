package de.gregorpoloczek.projectmaintainer.core.git.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class GithubCredentialProvider {

  @Value("file:./.credentials/github.properties")
  private Resource credentials;

  private final ConversionService conversionService;

  public GithubCredentialProvider(final ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public CredentialsProvider getCredentialProvider() {
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
}

package de.gregorpoloczek.projectmaintainer.core.git.github;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.git.common.GitCredentialsProvider;
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
public class GithubCredentialsProvider implements GitCredentialsProvider {

  @Value("file:./.credentials/github.properties")
  private Resource credentials;

  public static final Pattern GITHUB_PATTERN = Pattern.compile(
      "^\\Qhttps://github.com/\\E(?<owner>[^/]+)/(?<repository>[^.]+)\\.git$");
  private final ConversionService conversionService;

  public GithubCredentialsProvider(final ConversionService conversionService) {
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
  public FQPN getFQPN(final URI uri) {
    final Matcher matcher = GITHUB_PATTERN.matcher(uri.toString());
    if (matcher.matches()) {
      final String owner = matcher.group("owner");
      final String repository = matcher.group("repository");
      return FQPN.of("github", owner, repository);
    }
    throw new IllegalStateException(uri.toString());
  }

  @Override
  public boolean supports(final URI uri) {
    return uri.toString().startsWith("https://github.com");
  }
}

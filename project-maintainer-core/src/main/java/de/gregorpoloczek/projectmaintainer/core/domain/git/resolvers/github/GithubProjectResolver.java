package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.github;

import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class GithubProjectResolver extends AbstractProjectResolver {

  @Value("file:./.credentials/github.properties")
  private Resource credentials;

  public static final Pattern GITHUB_PATTERN = Pattern.compile(
      "^\\/(?<owner>[^/]+)/(?<repository>[^.]+)\\.git$");
  private final ConversionService conversionService;

  public GithubProjectResolver(final ConversionService conversionService) {
    this.conversionService = conversionService;
  }


  public CredentialsProvider getCredentialsProvider(WorkingCopy workingCopy) {
    final GithubCredentials credentials = workingCopy.getGitCredentials(GithubCredentials.class);
    return new UsernamePasswordCredentialsProvider(
        credentials.username(),
        credentials.password());
  }


  @Override
  public ProjectMetaData getProjectMetaData(final URI uri) {
    final Matcher matcher = GITHUB_PATTERN.matcher(uri.getPath());
    if (matcher.matches()) {
      final String owner = matcher.group("owner");
      final String repository = matcher.group("repository");

      return ProjectMetaData.builder()
          .uri(uri)
          .name(repository)
          .owner(owner)
          .fqpn("github", owner, repository)
          .build();
    }
    throw new IllegalStateException(uri.toString());
  }

  @Override
  public boolean supports(final URI uri) {
    return uri.toString().contains("github.com/");
  }
}

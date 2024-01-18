package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.aws;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codecommit.CodeCommitClient;

@Service
public class AWSCodeCommitProjectDiscovery implements ProjectDiscovery {

  @Override
  public List<URI> getURIs() {
    final CodeCommitClient client = CodeCommitClient.builder().region(Region.EU_CENTRAL_1).build();
    return client.listRepositories().repositories().stream()
        .map(r -> client.getRepository(b -> b.repositoryName(r.repositoryName())))
        .map(r -> r.repositoryMetadata().cloneUrlHttp())
        .map(http -> URI.create(http))
        .collect(Collectors.toList());
  }
}

package de.gregorpoloczek.projectmaintainer.git.provider.aws;

import de.gregorpoloczek.projectmaintainer.git.ProjectsDiscoveryAWSCodeCommitProperties;
import de.gregorpoloczek.projectmaintainer.core.common.properties.AWSCodeCommitLocation;
import de.gregorpoloczek.projectmaintainer.git.provider.common.PasswordResolverService;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.net.URI;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codecommit.CodeCommitClient;
import software.amazon.awssdk.services.codecommit.model.GetRepositoryResponse;
import software.amazon.awssdk.services.sts.StsClient;

@Service
@Slf4j
public class AWSCodeCommitProjectDiscovery implements ProjectDiscovery {

    private final PasswordResolverService passwordResolverService;
    private final ProjectsDiscoveryAWSCodeCommitProperties discoveryProperties;

    public AWSCodeCommitProjectDiscovery(
            PasswordResolverService passwordResolverService,
            ProjectsDiscoveryAWSCodeCommitProperties discoveryProperties) {
        this.passwordResolverService = passwordResolverService;
        this.discoveryProperties = discoveryProperties;
    }

    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        for (final AWSCodeCommitLocation location : discoveryProperties.getLocations()) {
            ProfileCredentialsProvider awsCredentialsProvider = ProfileCredentialsProvider.create(
                    location.getProfile());

            String username = location.getUsername();
            String password = passwordResolverService.getPassword("aws-codecommit", username);
            UsernamePasswordCredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(username, password);

            for (Region region : location.getRegions().stream().map(Region::of).toList()) {
                String accountId = getAccountId(awsCredentialsProvider, region);
                try (CodeCommitClient client = CodeCommitClient.builder()
                        .credentialsProvider(awsCredentialsProvider)
                        .region(region).build()) {

                    client.listRepositories().repositories().stream()
                            .map(r -> client.getRepository(b -> b.repositoryName(r.repositoryName())))
                            .map(GetRepositoryResponse::repositoryMetadata)
                            .forEach(r -> context.discovered(b -> b
                                            .fqpn(FQPN.of("aws-codecommit", accountId, region.id(), r.repositoryName()))
                                            .uri(URI.create(r.cloneUrlHttp()))
                                            .browserLink(
                                                    Optional.of(
                                                            "https://%s.console.aws.amazon.com/codesuite/codecommit/repositories/%s/browse?region=%s"
                                                                    .formatted(region.id(), r.repositoryName(), region.id()))
                                            )
                                            .name(r.repositoryName())
                                            .owner(accountId)
                                            .description(Optional.ofNullable(r.repositoryDescription()))
                                            .credentialsProvider(credentialsProvider)
                                    )
                            );
                }

            }


        }


    }

    private String getAccountId(ProfileCredentialsProvider awsCredentialsProvider, Region region) {
        try (StsClient stsClient = StsClient.builder()
                .region(region)
                .credentialsProvider(awsCredentialsProvider)
                .build()) {
            return stsClient.getCallerIdentity().account();
        }
    }
}

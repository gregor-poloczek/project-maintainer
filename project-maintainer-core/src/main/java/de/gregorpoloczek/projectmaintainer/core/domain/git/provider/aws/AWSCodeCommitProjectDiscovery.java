package de.gregorpoloczek.projectmaintainer.core.domain.git.provider.aws;

import de.gregorpoloczek.projectmaintainer.core.common.properties.AWSCodeCommitDiscoverySection;
import de.gregorpoloczek.projectmaintainer.core.common.properties.AWSCodeCommitLocation;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.git.provider.common.PasswordResolverService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
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
    private final ApplicationProperties applicationProperties;

    public AWSCodeCommitProjectDiscovery(
            PasswordResolverService passwordResolverService,
            ApplicationProperties applicationProperties) {
        this.passwordResolverService = passwordResolverService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        AWSCodeCommitDiscoverySection awsCodeCommitSection = applicationProperties.getProjects()
                .getDiscovery()
                .getAwsCodeCommit();
        if (awsCodeCommitSection == null) {
            log.info("Nothing configured for AWS CodeCommit.");
            return;
        }

        for (final AWSCodeCommitLocation location : awsCodeCommitSection.getLocations()) {
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

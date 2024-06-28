package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.aws;

import de.gregorpoloczek.projectmaintainer.core.common.properties.AWSCodeCommitDiscoverySection;
import de.gregorpoloczek.projectmaintainer.core.common.properties.AWSCodeCommitLocation;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codecommit.CodeCommitClient;
import software.amazon.awssdk.services.codecommit.model.GetRepositoryResponse;
import software.amazon.awssdk.services.sts.StsClient;

@Service
@Slf4j
public class AWSCodeCommitProjectDiscovery implements ProjectDiscovery {

    private final ApplicationProperties applicationProperties;
    @Value("file:./.credentials/aws-codecommit.properties")
    private Resource credentials;

    public AWSCodeCommitProjectDiscovery(final ConversionService conversionService,
            ApplicationProperties applicationProperties) {
        this.conversionService = conversionService;
        this.applicationProperties = applicationProperties;
    }

    private final ConversionService conversionService;

    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        AWSCodeCommitDiscoverySection awsCodeCommit = applicationProperties.getProjects()
                .getDiscovery()
                .getAwsCodeCommit();
        if (awsCodeCommit == null) {
            log.info("Nothing configured for AWS CodeCommit.");
            return;
        }

        final Properties passwords;
        try {
            passwords = this.conversionService.convert(
                    this.credentials.getContentAsString(StandardCharsets.UTF_8),
                    Properties.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (final AWSCodeCommitLocation location : awsCodeCommit.getLocations()) {
            ProfileCredentialsProvider awsCredentialsProvider = ProfileCredentialsProvider.create(
                    location.getProfile());

            String username = location.getUsername();
            String password = Optional.ofNullable(passwords.get(username)).map(String.class::cast)
                    .orElseThrow(() -> new IllegalStateException("Cannot find password for user " + username));
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

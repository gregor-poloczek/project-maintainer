package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.aws;

import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;

import java.net.URI;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codecommit.CodeCommitClient;
import software.amazon.awssdk.services.codecommit.model.GetRepositoryResponse;
import software.amazon.awssdk.services.sts.StsClient;

@Service
@Slf4j
public class AWSCodeCommitProjectDiscovery implements ProjectDiscovery<AWSCodeCommitProjectConnection> {
    @Override
    public void discoverProjects(final ProjectDiscoveryContext<AWSCodeCommitProjectConnection> context) {
        AWSCodeCommitProjectConnection connection = context.getConnection();

        ProfileCredentialsProvider awsCredentialsProvider = ProfileCredentialsProvider.create(
                connection.getProfile());

        for (Region region : Stream.of(connection.getRegion()).map(Region::of).toList()) {
            String accountId = getAccountId(awsCredentialsProvider, region);
            try (CodeCommitClient client = CodeCommitClient.builder()
                    .credentialsProvider(awsCredentialsProvider)
                    .region(region).build()) {

                client.listRepositories().repositories().stream()
                        .map(r -> client.getRepository(b -> b.repositoryName(r.repositoryName())))
                        .map(GetRepositoryResponse::repositoryMetadata)
                        .forEach(r -> context.discovered(b -> b
                                        .fqpn(FQPN.of(accountId, region.id(), r.repositoryName()))
                                        .uri(URI.create(r.cloneUrlHttp()))
                                        .browserLink(
                                                "https://%s.console.aws.amazon.com/codesuite/codecommit/repositories/%s/browse?region=%s"
                                                        .formatted(region.id(), r.repositoryName(), region.id())
                                        )
                                        .name(r.repositoryName())
                                        .owner(accountId)
                                        .description(r.repositoryDescription())
                                )
                        );
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

    @Override
    public boolean supports(String type) {
        return type.equals("aws-codecommit");
    }
}

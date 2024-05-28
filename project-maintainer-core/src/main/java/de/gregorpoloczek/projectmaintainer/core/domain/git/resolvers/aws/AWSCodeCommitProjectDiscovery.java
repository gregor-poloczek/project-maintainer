package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.aws;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codecommit.CodeCommitClient;

@Service
public class AWSCodeCommitProjectDiscovery implements ProjectDiscovery {

    @Value("file:./.credentials/aws-codecommit.properties")
    private Resource credentials;

    private static final Region REGION = Region.EU_CENTRAL_1;

    public AWSCodeCommitProjectDiscovery(final ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    private final ConversionService conversionService;

    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        final Properties credentials;
        try {
            credentials = this.conversionService.convert(
                    this.credentials.getContentAsString(StandardCharsets.UTF_8),
                    Properties.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String username = credentials.getProperty("username");
        final Matcher matcher = Pattern.compile("^(?<username>.+?)-at-(?<account>\\d+)$")
                .matcher(username);
        final String password = (String) credentials.get("password");

        UsernamePasswordCredentialsProvider credentialsProvider =
                new UsernamePasswordCredentialsProvider(username, password);

        if (!matcher.matches()) {
            throw new IllegalStateException("Cannot determined account from " + username);
        }
        final String accountId = matcher.group("account");

        final CodeCommitClient client = CodeCommitClient.builder().region(REGION).build();
        client.listRepositories().repositories().stream()
                .map(r -> client.getRepository(b -> b.repositoryName(r.repositoryName())))
                .map(r -> r.repositoryMetadata())
                .forEach(r -> context.discovered(b -> b
                                .fqpn(FQPN.of("aws-codecommit", accountId, REGION.id(), r.repositoryName()))
                                .uri(URI.create(r.cloneUrlHttp()))
                                .name(r.repositoryName())
                                .owner(accountId)
                                .description(Optional.ofNullable(r.repositoryDescription()))
                                .credentialsProvider(credentialsProvider)
                        )
                );
    }
}

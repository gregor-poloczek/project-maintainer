package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.bitbucket;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.AbstractProjectResolver;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import java.net.URI;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Service
public class BitbucketProjectResolver extends AbstractProjectResolver {

    public CredentialsProvider getCredentialsProvider(WorkingCopy workingCopy) {
        final BitbucketCredentials credentials = workingCopy.getGitCredentials(BitbucketCredentials.class);
        return new UsernamePasswordCredentialsProvider(
                credentials.username(),
                credentials.password());
    }


    @Override
    public boolean supports(final URI uri) {
        return uri.toString().contains("bitbucket.org/");
    }
}

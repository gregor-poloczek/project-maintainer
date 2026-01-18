package io.github.gregorpoloczek.projectmaintainer.scm.service.git;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.GitUsernamePasswordCredentialsFacet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportHttp;

import java.util.function.Function;

@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GitActionContext {
    @Getter
    Git git;
    Project project;
    @Getter
    BranchState branchState;
    CredentialsProvider credentialsProvider;

    public <T extends GitCommand<?>> T command(Function<Git, T> method) {
        T command = method.apply(git);
        if (command instanceof TransportCommand<?, ?> transportCommand) {
            // assign credential provider for the operation
            transportCommand.setCredentialsProvider(credentialsProvider);

            // adjust transport for further improvements, if possible
            transportCommand.setTransportConfigCallback(this::configureTransport);
        }
        return command;
    }

    private void configureTransport(Transport transport) {
        if (transport instanceof TransportHttp transportHttp) {
            // http connection is made to the git repository, a user name and password are involved,
            // use pre-emptive basic authentication, to avoid unnecessary rejections by the git
            // provide
            project.getFacet(BelongsToProjectConnection.class)
                    .map(BelongsToProjectConnection::getProjectConnection)
                    .flatMap(pC -> ((ProjectConnection) pC).getFacet(GitUsernamePasswordCredentialsFacet.class))
                    .ifPresent(f -> transportHttp.setPreemptiveBasicAuthentication(f.getUsername(), f.getPassword()));
        }
    }
}

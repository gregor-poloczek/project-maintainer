package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class TestProjectDiscovery implements ProjectDiscovery<TestProjectConnection> {

    @Override
    public boolean supports(String type) {
        return TestProjectConnection.TYPE.equals(type);
    }

    @Override
    public void discoverProjects(ProjectDiscoveryContext<TestProjectConnection> context) {
        for (String projectName : context.getConnection().getProjectNames()) {
            context.discovered(b -> b
                    .fqpn(FQPN.of(projectName))
                    .name(projectName)
                    .owner("integration-tests")
                    .defaultBranch("develop")
                    .uri(URI.create("https://git.example.invalid/" + projectName + ".git")));
        }
    }
}

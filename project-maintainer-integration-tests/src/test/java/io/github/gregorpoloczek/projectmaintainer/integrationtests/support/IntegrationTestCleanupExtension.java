package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import io.github.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

/**
 * Cleans up the state integration tests leave behind in the Spring context after each test, so that every test
 * starts from scratch:
 * <ul>
 *     <li>all workspaces (including their projects and working copies) are deleted</li>
 *     <li>the workspaces directory is emptied, which also removes leftovers not managed by the application (e.g.
 *     deliberately broken workspaces)</li>
 *     <li>the pull requests simulated by the {@link IntegrationTestFileSystemProjectDiscovery} are reset</li>
 * </ul>
 * Runs after the {@code @AfterEach} methods of the test class.
 */
public class IntegrationTestCleanupExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);

        WorkspaceService workspaceService = applicationContext.getBean(WorkspaceService.class);
        for (Workspace workspace : workspaceService.findWorkspaces()) {
            workspaceService.deleteWorkspace(workspace);
        }
        FileUtils.cleanDirectory(applicationContext.getBean(ApplicationProperties.class).getWorkspacesDirectory().toFile());

        applicationContext.getBean(IntegrationTestFileSystemProjectDiscovery.class).reset();
    }
}

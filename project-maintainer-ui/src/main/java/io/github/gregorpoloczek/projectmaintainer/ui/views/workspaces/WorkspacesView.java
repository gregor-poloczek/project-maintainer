package io.github.gregorpoloczek.projectmaintainer.ui.views.workspaces;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import io.github.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import io.github.gregorpoloczek.projectmaintainer.ui.common.HelpDialog;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import io.github.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import io.github.gregorpoloczek.projectmaintainer.ui.views.workspace.WorkspaceView;
import io.github.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.GitProviderIconComponent;
import lombok.NonNull;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;

import java.util.Comparator;
import java.util.List;

@Route(value = "/workspace", layout = MainLayout.class)
public class WorkspacesView extends VerticalLayout
        implements HelpDialog.MarkdownHelpFactory {

    private final Grid<WorkspaceItem> grid;
    private final ImageResolverService imageResolverService;
    private final ApplicationProperties applicationProperties;

    @Override
    public String createMarkdownHelp() {
        @Language("markdown")
        String code = """
                # Workspaces
                
                ## Purpose
                Workspaces bundle your maintained projects together. You need to create at least one workspace to manage your projects. Depending on your work, multiple workspaces can also be useful. For example, you might have one workspace dedicated to your own team’s work, while another is used for shared code across your company.
                
                Every feature in this application is executed within a single workspace at a time. To use these features, you need to **switch** to the desired workspace by pressing the *Use* button. A single project (Git repository) can be maintained in multiple workspaces. However, working copies of the same repository across different workspaces are completely independent.
                
                ## Lifecycle
                Create a workspace by providing a unique name and pressing the **Create New Workspace** button. To delete a workspace, switch to it and navigate to the **Workspace Settings** page.
                
                ## Data Storage
                Workspaces not only store settings but also cloned working copies of repositories. Each workspace has its own dedicated directory on the file system, located at `%s`. Deleting a workspace will remove the entire workspace directory from the file system, and this action cannot be undone.
                """;
        return code.formatted(applicationProperties.getWorkspacesDirectory());
    }

    @Value
    public static class WorkspaceItem {
        String id;
        String name;
        List<String> connectionTypes;
        long attachedProjects;
        long totalProjects;
    }

    private final WorkspaceService workspaceService;
    private final ProjectService projectService;
    private final WorkingCopyService workingCopyService;
    private final TextField newWorkspaceNameTextField;

    public WorkspacesView(
            WorkspaceService workspaceService,
            ProjectService projectService,
            WorkingCopyService workingCopyService,
            ImageResolverService imageResolverService,
            ApplicationProperties applicationProperties) {
        this.workspaceService = workspaceService;
        this.projectService = projectService;
        this.workingCopyService = workingCopyService;
        this.imageResolverService = imageResolverService;
        this.applicationProperties = applicationProperties;

        this.newWorkspaceNameTextField = new TextField();

        this.grid = createGrid();
        this.add(
                new H1("Workspaces"),
                new HorizontalLayout(
                        newWorkspaceNameTextField,
                        new Button("Create new workspace", MaterialIcons.CREATE.create(), e -> createWorkspace())),
                this.grid);
    }

    private Grid<WorkspaceItem> createGrid() {
        final Grid<WorkspaceItem> result;
        result = new Grid<>();
        result.addColumn(WorkspaceItem::getName)
                .setHeader("Name");
        result.addColumn(createConnectionsIconRenderer())
                .setHeader("Connections");
        result.addColumn(this.createProjectCountRenderer())
                .setHeader("Attached projects")
                .setTooltipGenerator(item -> "%d of available %d projects attached".formatted(item.getAttachedProjects(), item.getTotalProjects()));
        result.addColumn(this.createSwitchToWorkspaceRenderer())
                .setTooltipGenerator(i -> "Switch to workspace \"%s\"".formatted(i.getName()))
                .setFlexGrow(0)
                .setWidth("120px");
        return result;
    }


    private void openWorkspace(String workspaceId) {
        UI.getCurrent().navigate(WorkspaceView.class, new RouteParam(WorkspaceView.Parameters.WORKSPACE_ID, workspaceId));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.loadWorkspaces();
    }

    private void createWorkspace() {
        try {
            this.workspaceService.createWorkspace(newWorkspaceNameTextField.getValue());
            this.loadWorkspaces();
            VaadinUtils.Notifications.showSuccess("Workspace created");
        } catch (Exception e) {
            VaadinUtils.Notifications.showError("Workspace creation failed: %s".formatted(e.getMessage()));
            throw e;
        }
    }

    private void loadWorkspaces() {
        List<WorkspaceItem> list = workspaceService.findWorkspaces()
                .stream()
                .sorted(Comparator.comparing(Workspace::getName).thenComparing(Workspace::getId))
                .map(this::toWorkspaceItem)
                .toList();
        this.grid.setDataProvider(new ListDataProvider<>(list));

    }

    private @NonNull WorkspaceItem toWorkspaceItem(Workspace w) {
        List<String> connectionTypes = workspaceService.requireWorkspace(w.getId()).getProjectConnections().stream().map(ProjectConnection::getType).toList();
        List<Project> availableProjects = this.projectService.findAllByWorkspaceId(w.getId());
        long total = availableProjects.size();
        long attached = availableProjects.stream().filter(p -> this.workingCopyService.find(p).isPresent()).count();
        return new WorkspaceItem(w.getId(), w.getName(), connectionTypes, attached, total);
    }

    private ComponentRenderer<? extends Component, WorkspaceItem> createConnectionsIconRenderer() {
        return new ComponentRenderer<>(i -> {
            if (i.getConnectionTypes().isEmpty()) {
                return new Span("-");
            } else {
                var l = new HorizontalLayout();
                i.getConnectionTypes().stream().map(t -> new GitProviderIconComponent(imageResolverService, t)).forEach(l::add);
                return l;
            }
        });
    }

    private ComponentRenderer<Button, WorkspaceItem> createSwitchToWorkspaceRenderer() {
        return new ComponentRenderer<>(item -> new Button("Use", MaterialIcons.FOLDER_OPEN.create(), e -> openWorkspace(item.id)));
    }

    private ComponentRenderer<Span, WorkspaceItem> createProjectCountRenderer() {
        return new ComponentRenderer<>(item -> {
            if (item.getTotalProjects() == 0L) {
                return new Span("-");
            }
            return new Span("%d / %d".formatted(item.getAttachedProjects(), item.getTotalProjects()));
        });
    }
}

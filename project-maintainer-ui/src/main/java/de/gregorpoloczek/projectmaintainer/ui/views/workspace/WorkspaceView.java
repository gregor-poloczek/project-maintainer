package de.gregorpoloczek.projectmaintainer.ui.views.workspace;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.FieldSet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import de.gregorpoloczek.projectmaintainer.ui.common.progress.GenericOperationProgressBar;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.GitProviderIconComponent;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.ProjectConnectionForm;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.ProjectConnectionFormComponent;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.ProjectConnectionUIAdapter;
import de.gregorpoloczek.projectmaintainer.ui.views.workspaces.WorkspacesView;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@Route(value = "/workspace/:workspaceId", layout = MainLayout.class)
public class WorkspaceView extends VerticalLayout implements BeforeEnterObserver {
    private static final String PARAMETER_WORKSPACE_ID = "workspaceId";

    private final WorkspaceService workspaceService;

    private final H1 title;
    private final Button saveWorkspaceButton;
    private final Button deleteWorkspaceButton;
    private final Button discoverProjectsButton;
    private final GenericOperationProgressBar progressBar;
    private final Select<ConnectionFormProviderSelectItem> connectionFormProvidersSelect;
    private final VerticalLayout connectionFormsLayout;
    private final List<ProjectConnectionFormComponent<? extends ProjectConnection>> projectConnectionFormComponents = new ArrayList<>();
    private final ProjectService projectService;
    private final Text projectsCountsText;
    private final ImageResolverService imageResolverService;
    private String workspaceId;

    private final List<ProjectConnectionUIAdapter<?, ?>> projectConnectionUIAdapters;

    public WorkspaceView(
            ProjectService projectService,
            WorkspaceService workspaceService,
            ImageResolverService imageResolverService,
            List<ProjectConnectionUIAdapter<?, ?>> projectConnectionUIAdapters) {
        this.workspaceService = workspaceService;
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.projectConnectionUIAdapters = projectConnectionUIAdapters;

        this.title = new H1("Workspace");

        this.connectionFormsLayout = new VerticalLayout();
        this.connectionFormsLayout.setPadding(false);
        this.connectionFormsLayout.setWidthFull();

        Button addConnectionButton = new Button("Add Connection", e -> addNewConnection());

        // TODO [Workspaces] validity check
        this.saveWorkspaceButton = new Button("Save workspace", e -> saveConnections());
        this.deleteWorkspaceButton = new Button("Delete workspace", e -> deleteWorkspace());
        this.discoverProjectsButton = new Button("Discover projects", VaadinIcon.SEARCH.create(), e -> discoverProjects());
        this.progressBar = new GenericOperationProgressBar();
        this.progressBar.setWidthFull();
        this.connectionFormProvidersSelect = new Select<>();
        this.connectionFormProvidersSelect.setWidth("250px");
        this.connectionFormProvidersSelect.setEmptySelectionAllowed(false);
        this.connectionFormProvidersSelect.setRenderer(new ComponentRenderer<Component, ConnectionFormProviderSelectItem>(i -> {
            GitProviderIconComponent icon = new GitProviderIconComponent(this.imageResolverService, i.getType());
            icon.setHeight("20px");
            HorizontalLayout result = new HorizontalLayout(icon, new Span(i.getName()));
            result.setAlignItems(Alignment.CENTER);
            return result;
        }));

        List<ConnectionFormProviderSelectItem> items = projectConnectionUIAdapters
                .stream()
                .map(a -> new ConnectionFormProviderSelectItem(a.getTitle(), a.getType())).toList();

        this.connectionFormProvidersSelect.setItems(items);
        this.connectionFormProvidersSelect.setValue(((ListDataProvider<ConnectionFormProviderSelectItem>) this.connectionFormProvidersSelect.getDataProvider()).getItems().stream().findFirst().orElseThrow());

        projectsCountsText = new Text("");
        HorizontalLayout discoverLayout = new HorizontalLayout(this.discoverProjectsButton, this.progressBar);
        discoverLayout.setWidthFull();
        FieldSet projects = createGroup("Projects",
                new Span(new Text("Known projects: "), projectsCountsText), discoverLayout);
        projects.setWidthFull();
        FieldSet connections = createGroup("Connections", new HorizontalLayout(connectionFormProvidersSelect, addConnectionButton), connectionFormsLayout);
        connections.setWidthFull();
        this.add(title,
                projects,
                connections,
                new HorizontalLayout(this.saveWorkspaceButton, this.deleteWorkspaceButton));
    }

    private static @NonNull FieldSet createGroup(String label, Component... components) {
        FieldSet fieldSet = new FieldSet();
        fieldSet.add(new FieldSet.Legend(label));
        fieldSet.getStyle()
                .setBorderRadius("var(--lumo-border-radius-l)")
                .setBoxSizing(Style.BoxSizing.BORDER_BOX);
        fieldSet.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_20);
        VerticalLayout verticalLayout = new VerticalLayout(components);
        verticalLayout.setPadding(false);
        fieldSet.add(verticalLayout);
        return fieldSet;
    }

    private void deleteWorkspace() {
        try {
            this.toggleButtons(false);
            this.workspaceService.deleteWorkspace(this.workspaceService.requireWorkspace(this.workspaceId));
            // TODO [Workspaces] notification is now shown?
            Notification.show("Workspace deleted");
            UI.getCurrent().navigate(WorkspacesView.class);
        } catch (Exception e) {
            this.toggleButtons(true);
            Notification.show("Workspace deletion failed");
            throw e;
        }
    }

    private void addNewConnection() {
        String type = this.connectionFormProvidersSelect.getValue().getType();

        ProjectConnectionForm<ProjectConnection> form = ProjectConnectionForm.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .state(ProjectConnectionForm.State.NEW)
                .connection(null)
                .valid(false)
                .build();
        addRow(form);
    }

    private void toggleButtons(boolean desiredEnabledValue) {
        this.discoverProjectsButton.setEnabled(desiredEnabledValue);
        this.saveWorkspaceButton.setEnabled(desiredEnabledValue);
        this.deleteWorkspaceButton.setEnabled(desiredEnabledValue);
    }

    private void discoverProjects() {
        this.workspaceService
                .discoverProjects(this.workspaceService.requireWorkspace(this.workspaceId))
                .doOnSubscribe(s -> {
                    VaadinUtils.access(this, null, (p, c) -> {
                        toggleButtons(false);
                        progressBar.start();
                    });
                })
                .doOnTerminate(() -> {
                    VaadinUtils.access(this, null, (p, c) -> {
                        toggleButtons(true);
                        progressBar.stop();
                    });
                })
                .doOnNext(p -> VaadinUtils.access(this, p, (c, op) -> {
                    this.progressBar.update(op);
                }))
                .doOnError(e -> VaadinUtils.access(this, e, (c, t) -> {
                    Notification.show("Workspace project discovery failed");
                    log.error(t.getMessage(), t);
                }))
                .doOnComplete(() -> {
                    VaadinUtils.access(this, null, (c, t) -> {
                        Notification.show("Workspace project discovery completed");
                        this.loadProjects();
                    });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void saveConnections() {
        Workspace workspace = this.workspaceService.findWorkspace(workspaceId).orElseThrow();

        try {
            List<ProjectConnection> projectConnections = this.projectConnectionFormComponents.stream()
                    .map(ProjectConnectionFormComponent::getValue)
                    .map(ProjectConnectionForm::getConnection)
                    .map(ProjectConnection.class::cast)
                    .toList();

            this.workspaceService.updateConnections(workspace, projectConnections);
            Notification.show("Workspace saved");
        } catch (Exception ex) {
            Notification.show("Saving workspace failed");
            log.error(ex.getMessage(), ex);
        }

        // TODO [Workspaces] mark all as saved
    }


    private <T extends ProjectConnection> void addRow(ProjectConnectionForm<T> cf) {
        ProjectConnectionFormComponent<T> projectConnectionFormComponent = this.projectConnectionUIAdapters.stream()
                .filter(cFPP -> cFPP.supports(cf.getType()))
                .findFirst()
                .map(ProjectConnectionUIAdapter::createComponent)
                .map(c -> (ProjectConnectionFormComponent<T>) c)
                .orElseThrow(() -> new IllegalStateException("No connection provider for " + cf.getType()));

        projectConnectionFormComponent.addValueChangeListener(e -> {
            cf.setId(e.getValue().getId());
            cf.setConnection(e.getValue().getConnection());
            cf.setState(e.getValue().getState());
            cf.setValid(e.getValue().isValid());

            this.saveWorkspaceButton.setEnabled(this.projectConnectionFormComponents.stream()
                    .map(ProjectConnectionFormComponent::getValue)
                    .allMatch(ProjectConnectionForm::isValid));
        });
        projectConnectionFormComponent.setValue(cf);

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.setTooltipText("Delete connection");

        HorizontalLayout wrapper = new HorizontalLayout((Component) projectConnectionFormComponent, deleteButton);
        wrapper.getStyle()
                .setBorderRadius("var(--lumo-border-radius-l)");

        wrapper.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_20);
        HorizontalLayout row = new HorizontalLayout(wrapper, deleteButton);
        row.setFlexGrow(1, wrapper);
        row.setPadding(false);
        row.setWidthFull();

        deleteButton.addClickListener(e -> {
            this.connectionFormsLayout.remove(row);
            this.projectConnectionFormComponents.remove(projectConnectionFormComponent);
        });

        this.projectConnectionFormComponents.add(projectConnectionFormComponent);
        this.connectionFormsLayout.add(row);
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.workspaceId = beforeEnterEvent.getRouteParameters()
                .get(PARAMETER_WORKSPACE_ID)
                .orElseThrow();

        Workspace workspace = workspaceService.requireWorkspace(workspaceId);
        this.title.setText("Workspace - %s".formatted(workspace.getName()));

        this.connectionFormsLayout.removeAll();
        for (ProjectConnection projectConnection : workspace.getProjectConnections()) {
            addRow(ProjectConnectionForm.builder()
                    .id(projectConnection.getId())
                    .type(projectConnection.getType())
                    .state(ProjectConnectionForm.State.SAVED)
                    .connection(projectConnection)
                    .valid(true)
                    .build());
        }

        this.loadProjects();
    }

    private void loadProjects() {
        long projects = this.projectService.findAllByWorkspaceId(this.workspaceId)
                .stream()
                .count();
        this.projectsCountsText.setText(String.valueOf(projects));
    }
}

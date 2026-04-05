package io.github.gregorpoloczek.projectmaintainer.ui.views.projects;

import static io.github.gregorpoloczek.projectmaintainer.ui.common.MenuItemUtils.createMenuItem;
import static io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils.with;
import static io.github.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder.toComposableHolder;
import static java.util.function.Predicate.not;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import io.github.gregorpoloczek.projectmaintainer.ui.common.HelpDialog;
import io.github.gregorpoloczek.projectmaintainer.ui.common.progress.ProjectProgressBar;
import io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasProjectFilterComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import io.github.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.IconComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.OperationProgressComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectDescriptionComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectNameComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectWebsiteLinkComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.WorkingCopyStateComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasWorkingCopyFilterComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasWorkingCopy;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.experimental.UtilityClass;
import org.intellij.lang.annotations.Language;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Route(value = "/workspace/:workspaceId/projects", layout = MainLayout.class)
public class ProjectView extends VerticalLayout implements BeforeEnterObserver, HelpDialog.MarkdownHelpFactory {
    @Override
    public String createMarkdownHelp() {
        return """
                # Projects
                This view shows all the projects you have access to. To make them available for maintenance features, you need to *attach* them to the workspace first.
                
                ## Lifecycle of projects
                Attaching a project clones the repository as a local working copy. *Detaching* a project removes the working copy from the local file system. Some features do not automatically pull the latest changes from the origin, so you may need to update manually using the *Pull* button.
                
                ## Why are there no projects?
                If the list of projects is empty, you have either not set up a project connection for the current workspace or have not triggered a project discovery. Go to the *Workspace Settings* page and follow the instructions on how to set up a connection and trigger project discovery.
                """;
    }

    @UtilityClass
    public class Parameters {
        public static final String WORKSPACE_ID = "workspaceId";
    }

    private final transient ProjectService projectService;
    private final transient ImageResolverService imageResolverService;
    private final transient WorkingCopyService workingCopyService;
    private transient ComposableHolder<FQPN, ProjectItem> items;
    private final transient MenuBar menuBar;
    private final transient Grid<ProjectItem> grid;
    private final transient ListDataProvider<ProjectItem> dataProvider = new ListDataProvider<>(new ArrayList<>());

    private final transient Disposable.Swap currentOperation = Disposables.swap();
    private final transient ComposableFilterSearch<FQPN, ProjectItem> search = new ComposableFilterSearch<>(
            this.dataProvider);
    private final transient ProjectProgressBar projectProgressBar;
    private String workspaceId;

    public ProjectView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            WorkingCopyService workingCopyService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.workingCopyService = workingCopyService;

        this.projectProgressBar = new ProjectProgressBar(() -> currentOperation.update(Mono.empty().subscribe()));
        this.projectProgressBar.setWidthFull();
        this.grid = createGrid();

        this.menuBar = createMenuBar();
        this.add(with(new HorizontalLayout(
                        with(new VerticalLayout(new HorizontalLayout(
                                new HasWorkingCopyFilterComponent(search),
                                new HasProjectFilterComponent<>(search)
                        ), menuBar), VaadinUtils.NO_PADDING)), VaadinUtils.NO_PADDING),
                this.grid,
                this.projectProgressBar);
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private Grid<ProjectItem> createGrid() {
        final Grid<ProjectItem> result;
        result = new Grid<>(ProjectItem.class, false);
        result.setDataProvider(this.dataProvider);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(ProjectNameComponent.getRenderer()).setResizable(true).setHeader("Name");
        result.addColumn(ProjectWebsiteLinkComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(ProjectDescriptionComponent.getRenderer()).setResizable(true).setHeader("Description");
        result.addColumn(WorkingCopyStateComponent.getRenderer()).setResizable(true).setHeader("Working copy");
        result.addColumn(OperationProgressComponent.getRenderer());
        return result;
    }


    private MenuBar createMenuBar() {
        MenuBar result = new MenuBar();
        createMenuItem(result, "Clone project, and make it available for use.", MaterialIcons.ADD, "Attach", this::onAttachClick);
        createMenuItem(result, "Pull latest changes from SCM.", MaterialIcons.DOWNLOADING, "Pull", this::onPullClick);
        createMenuItem(result, "Remove cloned project from disk, and make it no longer available for use.", MaterialIcons.REMOVE, "Detach", this::onDetachClick);
        return result;
    }

    private void onOperationClick(Predicate<ProjectItem> predicate,
                                  Function<ProjectRelatable, Flux<ProjectOperationProgress<Void>>> operation, String label) {
        List<ProjectItem> relevantItems = grid.getSelectionModel()
                .getSelectedItems()
                .stream()
                .filter(predicate)
                .sorted()
                .toList();

        this.onBeforeOperation();
        this.projectProgressBar.start(relevantItems, label);

        Disposable subscription = Flux.fromIterable(relevantItems)
                .flatMap(item ->
                        operation.apply(item)
                                .onErrorComplete().subscribeOn(Schedulers.boundedElastic()))
                .doFinally(s -> VaadinUtils.access(this, ProjectView::onAfterOperation))
                .subscribe(p -> VaadinUtils.access(this, p, ProjectView::onUpdateEvent));
        currentOperation.update(subscription);
    }

    private void onDetachClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(
                this.workingCopyService::isAttached,
                this.workingCopyService::detachProject,
                "Detaching projects ...");
    }

    private void onAttachClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(
                not(this.workingCopyService::isAttached),
                this.workingCopyService::attachProject,
                "Attaching projects ...");
    }

    private void onPullClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(
                this.workingCopyService::isAttached,
                this.workingCopyService::pullProject,
                "Pulling projects ...");
    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.currentOperation.dispose();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        this.items = projectService.findAllByWorkspaceId(workspaceId).stream()
                .map(this::toProjectItem)
                .collect(toComposableHolder());
        this.dataProvider.getItems().addAll(items.getAll());
        this.dataProvider.refreshAll();
    }

    private void onUpdateEvent(ProjectOperationProgress<?> e) {
        if (e.getState().isTerminated()) {
            // only terminating operations can properly increase the progress, the other
            // git operations don't have reliable progress values
            this.projectProgressBar.update(e);
        }

        ProjectItem item = items.get(e.getFQPN());
        if (e.getState() == OperationProgress.State.DONE) {
            WorkingCopy workingCopy = this.workingCopyService.find(item).orElse(null);
            Image icon = this.imageResolverService.getProjectImage(item).orElse(null);
            item
                    .replaceTrait(HasWorkingCopy.class, t -> t.toBuilder().workingCopy(workingCopy).build())
                    .replaceTrait(HasIcon.class, t -> t.toBuilder()
                            .icon(icon)
                            .blurred(workingCopy == null)
                            .build())
                    .replaceTrait(HasOperationProgress.class, x -> HasOperationProgress.empty());
        } else {
            item.replaceTrait(HasOperationProgress.class, t -> t.toBuilder().operationProgress(e).build());
        }

        this.grid.getDataProvider().refreshItem(item);
        // refresh search
        this.search.refresh();
    }

    private ProjectItem toProjectItem(Project p) {
        ProjectMetaData metaData = p.getMetaData();
        Optional<WorkingCopy> workingCopy = this.workingCopyService.find(metaData.getFQPN());
        return ProjectItem.builder()
                .build()
                .addTrait(HasProject.class, () -> p)
                .addTrait(HasIcon.class, HasIcon.builder()
                        .icon(this.imageResolverService.getProjectImage(p).orElse(null))
                        .blurred(workingCopy.isEmpty())
                        .build())
                .addTrait(HasWorkingCopy.class,
                        HasWorkingCopy.builder().workingCopy(workingCopy.orElse(null)).build())
                .addTrait(HasOperationProgress.class, HasOperationProgress.empty());
    }

    private void onAfterOperation() {
        this.menuBar.setEnabled(true);
        this.projectProgressBar.stop();
    }

    private void onBeforeOperation() {
        this.menuBar.setEnabled(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.workspaceId = event.getRouteParameters().get(Parameters.WORKSPACE_ID).orElseThrow();
    }
}

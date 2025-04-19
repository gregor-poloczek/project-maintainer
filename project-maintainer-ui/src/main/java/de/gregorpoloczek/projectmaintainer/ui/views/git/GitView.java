package de.gregorpoloczek.projectmaintainer.ui.views.git;

import static de.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder.toComposableHolder;
import static java.util.function.Predicate.not;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.progress.ProjectProgressBar;
import de.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasProjectFilterComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.IconComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.OperationProgressComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectDescriptionComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectNameComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectWebsiteLinkComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.WorkingCopyStateComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasWorkingCopyFilterComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasWorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RouteAlias(value = "/", layout = MainLayout.class)
@Route(value = "/git", layout = MainLayout.class)
public class GitView extends VerticalLayout {

    private final transient ProjectService projectService;
    private final transient ImageResolverService imageResolverService;
    private final transient WorkingCopyService workingCopyService;
    private transient ComposableHolder<FQPN, ProjectItem> items;
    private final transient MenuBar menuBar;
    private final transient Grid<ProjectItem> grid;
    private final transient ListDataProvider<ProjectItem> dataProvider = new ListDataProvider<>(new ArrayList<>());

    private final transient Disposable.Swap currentOperation = Disposables.swap();
    private final transient ComposableFilterSearch<ProjectItem> search = new ComposableFilterSearch<>(
            this.dataProvider);
    private final transient ProjectProgressBar projectProgressBar;

    public GitView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            WorkingCopyService workingCopyService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.workingCopyService = workingCopyService;

        this.projectProgressBar = new ProjectProgressBar();
        this.projectProgressBar.setWidthFull();
        this.grid = createGrid();

        this.menuBar = createMenuBar();
        this.add(new HorizontalLayout(
                new HasWorkingCopyFilterComponent(search),
                new HasProjectFilterComponent<>(search)
        ), menuBar, grid, this.projectProgressBar);
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private Grid<ProjectItem> createGrid() {
        final Grid<ProjectItem> result;
        result = new Grid<>(ProjectItem.class, false);
        result.setDataProvider(this.dataProvider);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(ProjectNameComponent.getRenderer()).setHeader("Name");
        result.addColumn(ProjectWebsiteLinkComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(ProjectDescriptionComponent.getRenderer()).setHeader("Description");
        result.addColumn(WorkingCopyStateComponent.getRenderer()).setHeader("Working copy");
        result.addColumn(OperationProgressComponent.getRenderer());
        return result;
    }


    private MenuBar createMenuBar() {
        MenuBar result = new MenuBar();
        result.addItem("Attach", this::onAttachClick);
        result.addItem("Pull", this::onPullClick);
        result.addItem("Detach", this::onDetachClick);
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
                                .subscribeOn(Schedulers.parallel()))
                .doFinally(s -> VaadinUtils.access(this, GitView::onAfterOperation))
                .subscribe(p -> VaadinUtils.access(this, p, GitView::onUpdateEvent));
        currentOperation.update(subscription);
    }

    private void onDetachClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(
                this.workingCopyService::hasWorkspace,
                this.workingCopyService::wipeProject,
                "Detaching projects ...");
    }

    private void onAttachClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(
                not(this.workingCopyService::hasWorkspace),
                this.workingCopyService::cloneProject,
                "Attaching projects ...");
    }

    private void onPullClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(
                this.workingCopyService::hasWorkspace,
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

        this.items = projectService.findAll().stream()
                .map(this::toProjectItem)
                .collect(toComposableHolder());
        this.dataProvider.getItems().addAll(items.getAll());
        this.dataProvider.refreshAll();
    }

    private void onUpdateEvent(ProjectOperationProgress<?> e) {
        ProjectItem item = items.get(e.getFQPN());
        // TODO error handling

        if (e.getState().isTerminated()) {
            this.projectProgressBar.update(e);
        }
        if (e.getState() == OperationProgress.State.DONE) {
            WorkingCopy workingCopy = this.workingCopyService.find(item).orElse(null);
            Image icon = this.imageResolverService.getProjectImage(item).orElse(null);
            item
                    .replaceTrait(HasWorkingCopy.class, t -> t.toBuilder().workingCopy(workingCopy).build())
                    .replaceTrait(HasIcon.class, t -> t.toBuilder()
                            .icon(icon)
                            // TODO glaube das funktioniert hier nicht?
                            .blurred(workingCopy == null)
                            .build())
                    .replaceTrait(HasOperationProgress.class, _ -> HasOperationProgress.empty());
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
}

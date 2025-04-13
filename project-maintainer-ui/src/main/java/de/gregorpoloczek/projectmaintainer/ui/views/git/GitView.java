package de.gregorpoloczek.projectmaintainer.ui.views.git;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
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
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasWorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private Map<FQPN, ProjectItem> itemByFQPN;
    private final MenuBar menuBar;
    private final Grid<ProjectItem> grid;
    private final ListDataProvider<ProjectItem> dataProvider = new ListDataProvider<>(new ArrayList<>());

    private final transient Disposable.Swap currentOperation = Disposables.swap();


    public GitView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            WorkingCopyService workingCopyService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.workingCopyService = workingCopyService;

        var search = new ComposableFilterSearch<>(this.dataProvider);
        this.grid = createGrid();

        this.menuBar = createMenuBar();
        this.add(new HasProjectFilterComponent<>(search), menuBar, grid);
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

    private void onDetachClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(item -> item.requireTrait(HasWorkingCopy.class).getWorkingCopy().isPresent())
                .flatMap(item ->
                        this.workingCopyService.wipeProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doFinally(s -> this.unlockOperations(ui))
                .subscribe(p -> onUpdateEvent(p, ui));
        currentOperation.update(subscription);
    }

    private void onAttachClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();
        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(item -> item.requireTrait(HasWorkingCopy.class).getWorkingCopy().isEmpty())
                .flatMap(item ->
                        this.workingCopyService.cloneProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doFinally(s -> this.unlockOperations(ui))
                .subscribe(p -> onUpdateEvent(p, ui));

        currentOperation.update(subscription);
    }

    private void onPullClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(item -> item.requireTrait(HasWorkingCopy.class)
                        .getWorkingCopy()
                        .isPresent())
                .flatMap(item ->
                        this.workingCopyService.pullProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doFinally(s -> this.unlockOperations(ui))
                .subscribe(p -> onUpdateEvent(p, ui));

        currentOperation.update(subscription);
    }


    private void unlockOperations(UI ui) {
        if (!ui.isAttached()) {
            return;
        }
        ui.access(() -> this.menuBar.setEnabled(true));
    }

    private void lockOperations(UI ui) {
        if (!ui.isAttached()) {
            return;
        }
        ui.access(() -> this.menuBar.setEnabled(false));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.currentOperation.dispose();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<ProjectItem> items = projectService.findALl().stream()
                .map(this::toProjectItem)
                .toList();

        this.itemByFQPN = items.stream().collect(toMap(ProjectItem::getKey, identity()));

        this.dataProvider.getItems().addAll(items);
        this.dataProvider.refreshAll();
    }

    private void onUpdateEvent(ProjectOperationProgress<?> e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the mean time
            return;
        }
        ProjectItem item = itemByFQPN.get(e.getFQPN());
        current.access(() -> {
            // TODO error handling

            if (e.getState() == OperationProgress.State.DONE) {
                Project project = item.requireTrait(HasProject.class).getProject();
                WorkingCopy workingCopy = this.workingCopyService.find(e).orElse(null);
                Image icon = this.imageResolverService.getProjectImage(project).orElse(null);
                item
                        .replaceTrait(HasWorkingCopy.class, c -> c.toBuilder().workingCopy(workingCopy).build())
                        .replaceTrait(HasIcon.class, c -> c.toBuilder()
                                .icon(icon)
                                // TODO glaube das funktioniert hier nicht?
                                .blurred(workingCopy == null)
                                .build())
                        .replaceTrait(HasOperationProgress.class, c -> HasOperationProgress.empty());
            } else {
                item.replaceTrait(HasOperationProgress.class, c -> c.toBuilder().operationProgress(e).build());
            }

            this.grid.getDataProvider().refreshItem(item);
        });
    }

    private ProjectItem toProjectItem(de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project p) {
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

}

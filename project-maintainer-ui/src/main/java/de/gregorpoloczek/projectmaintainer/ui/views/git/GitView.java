package de.gregorpoloczek.projectmaintainer.ui.views.git;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasWorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasOperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasProject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
    private final TextField search;
    private final Grid<ProjectItem> grid;

    private final transient Disposable.Swap currentOperation = Disposables.swap();


    public GitView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            WorkingCopyService workingCopyService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.workingCopyService = workingCopyService;

        this.grid = createGrid();

        this.menuBar = createMenuBar();

        this.search = new TextField();
        this.search.setPlaceholder("Search");
        this.search.setValueChangeMode(ValueChangeMode.EAGER);
        this.search.addValueChangeListener(e -> {
            ListDataProvider<ProjectItem> dataProvider = (ListDataProvider<ProjectItem>) this.grid.getDataProvider();
            String query = e.getValue().toLowerCase();
            dataProvider.setFilter(
                    i -> StringUtils.isBlank(query) || i.matches(query));
            // TODO brauch ich das?
            dataProvider.refreshAll();
        });

        this.add(this.search, menuBar, grid);
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private Grid<ProjectItem> createGrid() {
        final Grid<ProjectItem> result;
        result = new Grid<>(ProjectItem.class, false);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(Renderers.getIconRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(Renderers.getProjectNameRenderer()).setHeader("Name");
        result.addColumn(Renderers.getProjectWebsiteLinkRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(Renderers.getProjectDescriptionRenderer()).setHeader("Description");
        result.addColumn(Renderers.getWorkingCopyRenderer()).setHeader("Working copy");
        result.addColumn(Renderers.getProgressBarRenderer());
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
                .filter(item -> item.requireComponent(HasWorkingCopy.class).getWorkingCopy().isPresent())
                .flatMap(item ->
                        this.workingCopyService.wipeProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnTerminate(() -> this.unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onUpdateEvent(p, ui));
        currentOperation.update(subscription);
    }

    private void onAttachClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();
        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .filter(item -> item.requireComponent(HasWorkingCopy.class).getWorkingCopy().isEmpty())
                .flatMap(item ->
                        this.workingCopyService.cloneProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnTerminate(() -> this.unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onUpdateEvent(p, ui));

        currentOperation.update(subscription);
    }

    private void onPullClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .filter(item -> item.requireComponent(HasWorkingCopy.class)
                        .getWorkingCopy()
                        .isPresent())
                .flatMap(item ->
                        this.workingCopyService.pullProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnTerminate(() -> unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onUpdateEvent(p, ui));

        currentOperation.update(subscription);
    }


    private void unlockOperations(UI ui) {
        ui.access(() -> this.menuBar.setEnabled(true));
    }

    private void lockOperations(UI ui) {
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

        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(
                        p -> p.requireComponent(HasProject.class).getProject().getMetaData().getFQPN(),
                        Function.identity()));

        this.grid.setItems(items);
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
                Project project = item.requireComponent(HasProject.class).getProject();
                WorkingCopy workingCopy = this.workingCopyService.find(e).orElse(null);
                Image icon = this.imageResolverService.getProjectImage(project).orElse(null);
                item
                        .replaceComponent(HasWorkingCopy.class, c -> c.toBuilder().workingCopy(workingCopy).build())
                        .replaceComponent(HasIcon.class, c -> c.toBuilder()
                                .icon(icon)
                                // TODO glaube das funktioniert hier nicht?
                                .blurred(workingCopy == null)
                                .build())
                        .replaceComponent(HasOperationProgress.class, c -> HasOperationProgress.empty());
            } else {
                item.replaceComponent(HasOperationProgress.class, c -> c.toBuilder().operationProgress(e).build());
            }

            this.grid.getDataProvider().refreshItem(item);
        });
    }

    private ProjectItem toProjectItem(de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project p) {
        ProjectMetaData metaData = p.getMetaData();
        Optional<WorkingCopy> workingCopy = this.workingCopyService.find(metaData.getFQPN());
        return ProjectItem.builder()
                .build()
                .addComponent(HasProject.class, () -> p)
                .addComponent(HasIcon.class, HasIcon.builder()
                        .icon(this.imageResolverService.getProjectImage(p).orElse(null))
                        .blurred(workingCopy.isEmpty())
                        .build())
                .addComponent(HasWorkingCopy.class,
                        HasWorkingCopy.builder().workingCopy(workingCopy.orElse(null)).build())
                .addComponent(HasOperationProgress.class, HasOperationProgress.empty());
    }

}

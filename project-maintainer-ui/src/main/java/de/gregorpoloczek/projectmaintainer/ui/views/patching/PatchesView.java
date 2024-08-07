package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchStopResult;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasOperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasProject;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasWorkingCopy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Route(value = "/patch", layout = MainLayout.class)
@JsModule("./diff2html-integration.js")
public class PatchesView extends VerticalLayout {

    private final transient ProjectService projectService;
    private final transient ImageResolverService imageResolverService;
    private final transient WorkingCopyService workingCopyService;
    private final transient PatchService patchService;
    private transient Map<FQPN, ProjectPatchItem> itemByFQPN = new HashMap<>();
    private final Grid<ProjectPatchItem> grid;
    private final MenuBar menuBar;
    private final ComboBox<PatchMetaData> patchesSelection;


    private final transient Disposable.Swap currentOperation = Disposables.swap();


    public PatchesView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            WorkingCopyService workingCopyService,
            PatchService patchService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.workingCopyService = workingCopyService;
        this.patchService = patchService;

        this.menuBar = createMenuBar();

        this.grid = createGrid();
        this.grid.setItemDetailsRenderer(
                new ComponentRenderer<>(item -> item.getPatchExecutionResult()
                        .map(PatchExecutionResultDetailView::new)
                        .map(Component.class::cast)
                        .or(() -> item.getPatchStopResult()
                                .map(PatchStopResultDetailView::new))
                        .orElseGet(Div::new)));

        this.patchesSelection = new ComboBox<>();
        this.patchesSelection.setWidth("400px");
        this.patchesSelection.setItemLabelGenerator(PatchMetaData::getId);
        this.patchesSelection.setRequired(true);
        this.patchesSelection.addValueChangeListener(e -> {
            itemByFQPN.values().forEach(item -> {
                item.clearResult();
                this.grid.setDetailsVisible(item, false);
                this.grid.getListDataView().refreshItem(item);
            });
        });

        this.add(this.patchesSelection, this.menuBar, this.grid);
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private Grid<ProjectPatchItem> createGrid() {
        final Grid<ProjectPatchItem> result;
        result = new Grid<>(ProjectPatchItem.class, false);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(Renderers.getIconRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(Renderers.getProjectNameRenderer()).setHeader("Name");
        result.addColumn(ProjectPatchItem::getState).setHeader("State");
        result.addColumn(Renderers.getProgressBarRenderer());
        return result;
    }


    private MenuBar createMenuBar() {
        MenuBar result = new MenuBar();
        MenuItem preview = result.addItem("Preview", this::onPreviewClick);
        MenuItem apply = result.addItem("Apply", this::onApplyClick);
        MenuItem stop = result.addItem("Stop", this::onStopClick);

        result.setTooltipText(preview, "Preview the patch in selected projects. Will not cause any changes.");
        result.setTooltipText(apply,
                "Apply patch to projects. Will create pull requests when source code is actually changed.");
        result.setTooltipText(stop,
                "Reverts non merged patches by the created pull request and deleting the existing remote branch.");

        return result;
    }


    private void unlockOperations(UI ui) {
        ui.access(() -> {
            this.menuBar.setEnabled(true);
            this.patchesSelection.setEnabled(true);
        });
    }

    private void lockOperations(UI ui) {
        ui.access(() -> {
            this.menuBar.setEnabled(false);
            this.patchesSelection.setEnabled(false);
        });
    }

    private void onStopClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(this::isItemHasWorkingCopy)
                .doOnNext(this::clearItem)
                .flatMap(item ->
                        this.patchService.stopPatch(item, this.patchesSelection.getValue().getId())
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnTerminate(() -> this.unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onPatchStopProgress(p, ui));

        currentOperation.update(subscription);
    }

    private boolean isItemHasWorkingCopy(ProjectPatchItem item) {
        return item.requireComponent(HasWorkingCopy.class)
                .getWorkingCopy()
                .isPresent();
    }

    private void onPreviewClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(this::isItemHasWorkingCopy)
                .doOnNext(this::clearItem)
                .flatMap(item ->
                        this.patchService.previewPatch(item, this.patchesSelection.getValue().getId())
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(_ -> this.lockOperations(ui))
                .doOnTerminate(() -> this.unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onPatchExecutionProgress(p, ui));

        currentOperation.update(subscription);
    }

    private void onApplyClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(this::isItemHasWorkingCopy)
                .doOnNext(this::clearItem)
                .flatMap(item ->
                        this.patchService.applyPatch(item, this.patchesSelection.getValue().getId())
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(_ -> this.lockOperations(ui))
                .doOnTerminate(() -> this.unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onPatchExecutionProgress(p, ui));

        currentOperation.update(subscription);
    }

    private void clearItem(ProjectPatchItem item) {
        item.clearResult();
        this.grid.setDetailsVisible(item, false);
        this.grid.getDataProvider().refreshItem(item);
    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.currentOperation.dispose();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<PatchMetaData> patches = patchService.getAvailablePatches();
        this.patchesSelection.setItems(patches);
        this.patchesSelection.setValue(patches.getFirst());

        List<ProjectPatchItem> items = projectService.findALl().stream()
                .filter(p -> workingCopyService.find(p).isPresent())
                .map(this::toProjectItem)
                .toList();

        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(ProjectPatchItem::getFQPN, Function.identity()));

        this.grid.setItems(items);
    }

    private void onPatchStopProgress(ProjectOperationProgress<PatchStopResult> e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the meantime
            return;
        }
        ProjectPatchItem item = itemByFQPN.get(e.getFQPN());
        current.access(() -> {
            // TODO error handling

            if (e.getState() == State.SCHEDULED) {
                item.setPatchExecutionResult(null);
            } else if (e.getState() == OperationProgress.State.DONE) {
                item.replaceComponent(HasOperationProgress.class, c -> HasOperationProgress.empty());
                item.setPatchExecutionResult(null);
                item.setPatchStopResult(e.getResult());
                this.grid.setDetailsVisible(item, true);
            } else {
                item.replaceComponent(HasOperationProgress.class,
                        c -> c.toBuilder().operationProgress(e).build());
            }

            this.grid.getDataProvider().refreshItem(item);
        });
    }

    private void onPatchExecutionProgress(ProjectOperationProgress<PatchExecutionResult> e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the meantime
            return;
        }
        ProjectPatchItem item = itemByFQPN.get(e.getFQPN());
        current.access(() -> {
            // TODO error handling

            if (e.getState() == State.SCHEDULED) {
                item.setPatchExecutionResult(null);
            } else if (e.getState() == OperationProgress.State.DONE) {
                item.replaceComponent(HasOperationProgress.class, c -> HasOperationProgress.empty());
                item.setPatchExecutionResult(e.getResult());
                item.setPatchStopResult(null);
                this.grid.setDetailsVisible(item, true);
            } else {
                item.replaceComponent(HasOperationProgress.class,
                        c -> c.toBuilder().operationProgress(e).build());
            }

            this.grid.getDataProvider().refreshItem(item);
        });
    }

    private ProjectPatchItem toProjectItem(Project p) {
        ProjectMetaData metaData = p.getMetaData();
        Optional<WorkingCopy> workingCopy = this.workingCopyService.find(metaData.getFQPN());
        return ProjectPatchItem.builder()
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

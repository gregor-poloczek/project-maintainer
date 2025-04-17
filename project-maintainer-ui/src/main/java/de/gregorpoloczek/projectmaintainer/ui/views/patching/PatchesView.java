package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
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
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResult;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchStopResult;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasProjectFilterComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.IconComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.OperationProgressComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectNameComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasWorkingCopy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import reactor.core.Disposable;
import reactor.core.Disposable.Swap;
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
    private final ListDataProvider<ProjectPatchItem> dataProvider = new ListDataProvider<>(new ArrayList<>());
    private final MenuBar menuBar;
    private final ComboBox<PatchMetaData> patchesSelection;


    private final transient Swap currentOperation = Disposables.swap();


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

        var search = new ComposableFilterSearch<>(this.dataProvider);
        this.grid = createGrid();
        this.grid.setItemDetailsRenderer(
                new ComponentRenderer<>(item -> switch (item.getPatchOperationResult().orElse(null)) {
                    case PatchExecutionResult per -> new PatchExecutionResultDetailView(per);
                    case PatchStopResult per -> new PatchStopResultDetailView(per);
                    case null -> new Div("null");
                    default -> new Div();
                }));

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

        this.add(this.patchesSelection, new HasProjectFilterComponent<>(search), this.menuBar, this.grid);
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private Grid<ProjectPatchItem> createGrid() {
        final Grid<ProjectPatchItem> result;
        result = new Grid<>(ProjectPatchItem.class, false);
        result.setDataProvider(this.dataProvider);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(ProjectNameComponent.getRenderer()).setHeader("Name");
        result.addColumn(ProjectPatchItem::getState).setHeader("State");
        result.addColumn(OperationProgressComponent.getRenderer());
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
        if (!ui.isAttached()) {
            return;
        }
        ui.access(() -> {
            this.menuBar.setEnabled(true);
            this.patchesSelection.setEnabled(true);
        });
    }

    private void lockOperations(UI ui) {
        if (!ui.isAttached()) {
            return;
        }
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
                .doOnNext(p -> this.onPatchOperationProgress(p, ui))
                .doFinally(s -> this.unlockOperations(ui))
                .subscribe();

        this.currentOperation.update(subscription);
    }

    private boolean isItemHasWorkingCopy(ProjectPatchItem item) {
        return item.requireTrait(HasWorkingCopy.class)
                .getWorkingCopy()
                .isPresent();
    }

    private void onPreviewClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(this::isItemHasWorkingCopy)
                .doOnNext(this::clearItem)
                .flatMap(item -> this.patchService.previewPatch(item, this.patchesSelection.getValue().getId())
                        .doOnError(t -> onError(item, t, ui))
                        .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnNext(p -> this.onPatchOperationProgress(p, ui))
                .doFinally(s -> this.unlockOperations(ui))
                .subscribe();

        this.currentOperation.update(subscription);
    }

    private void onError(ProjectPatchItem item, Throwable throwable, UI ui) {
        ui.access(() -> {
            item.setThrowable(throwable);
            item.replaceTrait(HasOperationProgress.class, c -> HasOperationProgress.empty());
            this.grid.getDataProvider().refreshItem(item);
        });
    }

    private void onApplyClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .sort()
                .filter(this::isItemHasWorkingCopy)
                .doOnNext(this::clearItem)
                .flatMap(item ->
                        this.patchService.applyPatch(item, this.patchesSelection.getValue().getId())
                                .doOnError(t -> onError(item, t, ui))
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnNext(p -> onPatchOperationProgress(p, ui))
                .doFinally(s -> this.unlockOperations(ui))
                .subscribe();

        this.currentOperation.update(subscription);
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

        List<ProjectPatchItem> items = projectService.findAll().stream()
                .filter(p -> workingCopyService.find(p).isPresent())
                .map(this::toProjectItem)
                .toList();

        this.itemByFQPN = items.stream().collect(toMap(ProjectPatchItem::getFQPN, identity()));

        this.dataProvider.getItems().addAll(items);
        this.dataProvider.refreshAll();
    }

    private void onPatchOperationProgress(ProjectOperationProgress<? extends PatchOperationResult> progress,
            UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the meantime
            return;
        }
        ProjectPatchItem item = itemByFQPN.get(progress.getFQPN());
        current.access(() -> {
            // TODO error handling
            if (progress.getState() == State.SCHEDULED) {
                item.setPatchOperationResult(null);
            } else if (progress.getState() == State.DONE) {
                item.replaceTrait(HasOperationProgress.class, c -> HasOperationProgress.empty());
                item.setPatchOperationResult(progress.getResult());
                this.grid.setDetailsVisible(item, true);
            } else {
                item.replaceTrait(HasOperationProgress.class,
                        c -> c.toBuilder().operationProgress(progress).build());
            }

            this.grid.getDataProvider().refreshItem(item);
        });
    }

    private ProjectPatchItem toProjectItem(Project p) {
        ProjectMetaData metaData = p.getMetaData();
        Optional<WorkingCopy> workingCopy = this.workingCopyService.find(metaData.getFQPN());
        return ProjectPatchItem.builder()
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

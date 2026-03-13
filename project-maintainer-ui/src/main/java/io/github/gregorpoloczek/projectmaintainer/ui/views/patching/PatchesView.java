package io.github.gregorpoloczek.projectmaintainer.ui.views.patching;

import static io.github.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder.toComposableHolder;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResult;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchStopResult;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import io.github.gregorpoloczek.projectmaintainer.ui.common.progress.ProjectProgressBar;
import io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasProjectFilterComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import io.github.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.IconComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.OperationProgressComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectNameComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasWorkingCopy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.BooleanUtils;
import org.jspecify.annotations.NonNull;
import reactor.core.Disposable;
import reactor.core.Disposable.Swap;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Route(value = "/workspace/:workspaceId/patch", layout = MainLayout.class)
@JsModule("./diff2html-integration.js")
public class PatchesView extends VerticalLayout implements BeforeEnterObserver {

    private final transient ProjectService projectService;
    private final transient ImageResolverService imageResolverService;
    private final transient WorkingCopyService workingCopyService;
    private final transient PatchService patchService;
    private final ProjectProgressBar projectProgressBar;
    private final ComposableFilterSearch<FQPN, ProjectPatchItem> search;
    private final PatchParameterArgumentsComponent patchParameterArgumentsComponent;
    private String workspaceId;
    private transient ComposableHolder<FQPN, ProjectPatchItem> items = ComposableHolder.emptyHolder();
    private final Grid<ProjectPatchItem> grid;
    private final ListDataProvider<ProjectPatchItem> dataProvider = new ListDataProvider<>(new ArrayList<>());
    private final MenuBar menuBar;
    private final ComboBox<PatchMetaData> patchesSelection;
    private int diffContextSize = 10;


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
        this.menuBar.setEnabled(false);

        this.projectProgressBar = new ProjectProgressBar();
        this.projectProgressBar.setWidthFull();

        this.patchParameterArgumentsComponent = new PatchParameterArgumentsComponent();

        Details details = new Details("Parameters", this.patchParameterArgumentsComponent);
        details.setOpened(true);

        search = new ComposableFilterSearch<>(this.dataProvider);

        Checkbox hideNoOpCheckbox = new Checkbox();
        hideNoOpCheckbox.setLabel("Hide No-Op");
        var handler = search.add(item -> {
            if (BooleanUtils.isFalse(hideNoOpCheckbox.getValue())) {
                return true;
            }
            if (item.getPatchOperationResult().isEmpty()) {
                return true;
            }
            return !(item.getPatchOperationResult().get().getDetail() instanceof PatchExecutionResult.NoopResultDetail);
        });
        hideNoOpCheckbox.addAttachListener(e1 -> hideNoOpCheckbox.addValueChangeListener(e2 -> handler.refresh()));
        hideNoOpCheckbox.addDetachListener(e -> handler.remove());

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
        this.patchesSelection.addValueChangeListener(x -> onPatchSelected(x.getValue()));

        Component diffContextSizeRangeInput = createDiffContextSizeRangeInput();

        HorizontalLayout horizontalLayout = new HorizontalLayout(
                this.patchesSelection,
                new HasProjectFilterComponent<>(search),
                hideNoOpCheckbox, diffContextSizeRangeInput);
        horizontalLayout.setAlignItems(Alignment.CENTER);
        this.add(horizontalLayout,
                details,
                this.menuBar,
                this.grid,
                this.projectProgressBar
        );
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private @NonNull Component createDiffContextSizeRangeInput() {
        Span valueSpan = new Span(this.diffContextSize + "");
        RangeInput result = new RangeInput();
        result.setMin(2);
        result.setMax(30);
        result.setStep(1.0);
        result.addValueChangeListener(e -> {
            this.diffContextSize = (int) Math.floor(e.getValue());
            valueSpan.setText("" + this.diffContextSize);
        });


        HorizontalLayout layout = new HorizontalLayout(new Span("Diff lines: "), result, valueSpan);
        layout.setPadding(false);
        return layout;
    }

    private void onPatchSelected(PatchMetaData value) {
        this.menuBar.setEnabled(value != null);
        items.getAll().forEach(item -> {
            item.clearResult();
            this.grid.setDetailsVisible(item, false);
            this.dataProvider.refreshItem(item);
        });
        search.refresh();

        if (value != null) {
            this.patchParameterArgumentsComponent.setParameters(value.getPatchParameters());
        }

    }

    private Grid<ProjectPatchItem> createGrid() {
        final Grid<ProjectPatchItem> result;
        result = new Grid<>(ProjectPatchItem.class, false);
        result.setDataProvider(this.dataProvider);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(ProjectNameComponent.getRenderer()).setHeader("Name");
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


    private void onAfterOperation() {
        this.menuBar.setEnabled(true);
        this.patchesSelection.setEnabled(true);
        this.projectProgressBar.stop();
    }

    private void onBeforeOperation() {
        this.menuBar.setEnabled(false);
        this.patchesSelection.setEnabled(false);
    }

    private <T extends PatchOperationResult> void onOperationClick(
            Function<ProjectRelatable, Flux<ProjectOperationProgress<T>>> operation,
            String label) {
        List<ProjectPatchItem> relevantItems = grid.getSelectionModel().getSelectedItems().stream()
                .sorted().toList();
        projectProgressBar.start(relevantItems, label);

        // TODO [Patching] alle betroffenen projekte zählen und anzeigen

        Disposable subscription = Flux.fromIterable(relevantItems)
                .doOnNext(this::clearItem)
                .flatMap(item ->
                        operation.apply(item)
                                // TODO [Patching] broken package is not visible in view
                                .onErrorComplete()
                                .subscribeOn(Schedulers.boundedElastic()))
                .doOnSubscribe(x -> VaadinUtils.access(this, PatchesView::onBeforeOperation))
                .doOnNext(p -> VaadinUtils.access(this, p, PatchesView::onPatchOperationProgress))
                .doFinally(x -> VaadinUtils.access(this, PatchesView::onAfterOperation))
                .subscribe();

        this.currentOperation.update(subscription);
    }

    private void onStopClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(item -> this.patchService.stopPatch(
                        item,
                        this.patchesSelection.getValue().getId(),
                        this.patchParameterArgumentsComponent.getValues().values()),
                "Stopping patch process ...");
    }

    private void onPreviewClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(item -> this.patchService.previewPatch(
                        item,
                        this.patchesSelection.getValue().getId(),
                        this.patchParameterArgumentsComponent.getValues().values(),
                        this.diffContextSize),
                "Previewing patch ...");
    }

    private void onApplyClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(item -> this.patchService.applyPatch(
                        item,
                        this.patchesSelection.getValue().getId(),
                        this.patchParameterArgumentsComponent.getValues().values(),
                        this.diffContextSize),
                "Applying patch ...");
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
        this.patchesSelection.setValue(patches.stream().findFirst().orElse(null));
        if (patches.isEmpty()) {
            this.patchesSelection.setEnabled(false);
        }

        this.items = projectService.findAllByWorkspaceId(this.workspaceId).stream()
                .filter(workingCopyService::isAttached)
                .map(this::toProjectItem)
                .collect(toComposableHolder());

        this.dataProvider.getItems().addAll(items.getAll());
        this.dataProvider.refreshAll();
    }

    private void onPatchOperationProgress(ProjectOperationProgress<? extends PatchOperationResult> progress) {
        ProjectPatchItem item = items.get(progress.getFQPN());
        this.projectProgressBar.update(progress);

        if (progress.getState() == State.DONE) {
            this.grid.setDetailsVisible(item, true);
        }
        item.replaceTrait(HasOperationProgress.class,
                c -> c.toBuilder().operationProgress(progress).build());

        this.dataProvider.refreshAll();
        this.dataProvider.refreshItem(item);
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.workspaceId = event.getRouteParameters().get("workspaceId").orElseThrow();
    }
}

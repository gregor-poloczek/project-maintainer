package io.github.gregorpoloczek.projectmaintainer.ui.views.patching;

import static io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils.with;
import static io.github.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder.toComposableHolder;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
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
import com.vaadin.flow.component.icon.Icon;
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
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResultDetail;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.UnifiedDiffFile;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.gregorpoloczek.projectmaintainer.ui.views.patching.components.StatisticsComponent;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;
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

    private String workspaceId;

    private final ProjectProgressBar projectProgressBar;
    private final ComposableFilterSearch<FQPN, ProjectPatchItem> search;
    private final PatchParameterArgumentsComponent patchParameterArgumentsComponent;
    private final Grid<ProjectPatchItem> grid;
    private final MenuBar menuBar;
    private final ComboBox<PatchMetaData> patchesSelection;
    private final StatisticsComponent statisticsComponent;

    private transient ComposableHolder<FQPN, ProjectPatchItem> items = ComposableHolder.emptyHolder();
    private final ListDataProvider<ProjectPatchItem> dataProvider = new ListDataProvider<>(new ArrayList<>());
    private int diffContextSize = 10;
    private final transient Swap currentOperation = Disposables.swap();

    enum PatchOperationType {
        PREVIEW, APPLY, STOP
    }

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

        this.search = new ComposableFilterSearch<>(this.dataProvider);

        this.grid = createGrid();

        this.patchesSelection = new ComboBox<>();
        this.patchesSelection.setWidth("400px");
        this.patchesSelection.setItemLabelGenerator(PatchMetaData::getId);
        this.patchesSelection.setRequired(true);
        this.patchesSelection.addValueChangeListener(x -> onPatchSelected(x.getValue()));

        this.statisticsComponent = new StatisticsComponent();
        this.add(with(new HorizontalLayout(
                        this.patchesSelection,
                        new HasProjectFilterComponent<>(search),
                        this.createHideNoOpCheckbox(),
                        this.createDiffContextSizeRangeInput()), VaadinUtils.ALIGN_ITEMS_CENTER),
                with(new Details("Parameters", this.patchParameterArgumentsComponent), d -> d.setOpened(true)),
                with(new HorizontalLayout(this.menuBar, this.statisticsComponent),
                        VaadinUtils.NO_PADDING, VaadinUtils.ALIGN_ITEMS_CENTER),
                this.grid,
                this.projectProgressBar
        );
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private @NonNull Checkbox createHideNoOpCheckbox() {
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
        return hideNoOpCheckbox;
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


        return with(new HorizontalLayout(new Span("Diff lines: "), result, valueSpan), VaadinUtils.NO_PADDING);
    }

    private void onPatchSelected(PatchMetaData value) {
        this.menuBar.setEnabled(value != null);
        this.items.getAll().forEach(item -> {
            item.clearResult();
            this.grid.setDetailsVisible(item, false);
            this.dataProvider.refreshItem(item);
        });
        this.search.refresh();

        this.statisticsComponent.clear();

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
        result.addColumn(createPatchDetailRenderer());
        result.addColumn(OperationProgressComponent.getRenderer());

        result.setItemDetailsRenderer(
                new ComponentRenderer<>(item -> switch (item.getPatchOperationResult().orElse(null)) {
                    case PatchExecutionResult per -> new PatchExecutionResultDetailView(per);
                    case PatchStopResult per -> new PatchStopResultDetailView(per);
                    case null -> null;
                    default -> new Div();
                }));
        return result;
    }

    private @NonNull ComponentRenderer<? extends Component, ProjectPatchItem> createPatchDetailRenderer() {
        return new ComponentRenderer<>(i -> {
            if (i.getPatchOperationResult().isEmpty()) {
                // nothing happened
                return new Span("");
            }
            PatchOperationResultDetail detail = i.getPatchOperationResult().get().getDetail();
            if (detail instanceof PatchExecutionResult.PreviewGeneratedResultDetail pgr) {
                // Small statistic with occurrences of file modifications
                Map<UnifiedDiffFile.Type, Long> countByType = pgr.getUnifiedDiff().getFiles().stream().collect(Collectors.groupingBy(UnifiedDiffFile::getType, Collectors.counting()));

                VerticalLayout layout = with(new VerticalLayout(), VaadinUtils.NO_PADDING);
                for (UnifiedDiffFile.Type type : countByType.keySet()) {
                    String label = StringUtils.capitalize(type.name().toLowerCase());
                    layout.add(new Span("%s: %d".formatted(label, countByType.get(type))));
                }
                return layout;
            }

            return new Span("");
        });
    }


    private MenuBar createMenuBar() {
        MenuBar result = new MenuBar();
        createMenuItem(result, this::onPreviewClick, MaterialIcons.VISIBILITY, "Preview",
                "Preview the patch in selected projects. Will not cause any changes.");

        createMenuItem(result, this::onApplyClick, MaterialIcons.APPROVAL, "Apply",
                "Apply patch to projects. Will create pull requests when source code is actually changed.");

        createMenuItem(result, this::onStopClick, MaterialIcons.CANCEL, "Stop",
                "Reverts non merged patches of the created pull request and deleting the existing remote branch." +
                        " If a branch name other then the default branch name was used during application of the patch, it must be re-entered " +
                        " in order to stop close the correct pull requests.");
        return result;
    }

    private void createMenuItem(MenuBar menuBar, ComponentEventListener<ClickEvent<MenuItem>> callback, MaterialIcons icon, String label, String tooltipText) {
        Icon iconComponent = icon.create();
        iconComponent.getStyle().setMarginRight("var(--lumo-space-s)");
        MenuItem preview = menuBar.addItem(iconComponent, callback);
        preview.add(new Text(label));
        menuBar.setTooltipText(preview, tooltipText);
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
            PatchOperationType operationType,
            Function<ProjectRelatable, Flux<ProjectOperationProgress<T>>> operation,
            String label) {

        List<ProjectPatchItem> relevantItems = this.grid.getSelectionModel().getSelectedItems().stream()
                .sorted().toList();
        this.projectProgressBar.start(relevantItems, label);

        Disposable subscription = Flux.fromIterable(relevantItems)
                .doOnNext(this::clearItem)
                .doOnNext(this.statisticsComponent::clear)
                .flatMap(item ->
                        operation.apply(item)
                                .onErrorComplete()
                                .subscribeOn(Schedulers.boundedElastic()))
                .doOnSubscribe(x -> VaadinUtils.access(this, PatchesView::onBeforeOperation))
                .doOnNext(p -> VaadinUtils.access(this, p, (v, pr) -> v.onPatchOperationProgress(operationType, pr)))
                .doFinally(x -> VaadinUtils.access(this, PatchesView::onAfterOperation))
                .subscribe();

        this.currentOperation.update(subscription);
    }

    private void onStopClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(PatchOperationType.STOP, item -> this.patchService.stopPatch(
                        item,
                        this.patchesSelection.getValue().getId(),
                        this.patchParameterArgumentsComponent.getValues().values()),
                "Stopping patch process ...");
    }

    private void onPreviewClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(PatchOperationType.PREVIEW, item -> this.patchService.previewPatch(
                        item,
                        this.patchesSelection.getValue().getId(),
                        this.patchParameterArgumentsComponent.getValues().values(),
                        this.diffContextSize),
                "Previewing patch ...");
    }

    private void onApplyClick(ClickEvent<MenuItem> event) {
        this.onOperationClick(PatchOperationType.APPLY, item -> this.patchService.applyPatch(
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
        this.patchesSelection.setEnabled(!patches.isEmpty());

        this.items = this.projectService.findAllByWorkspaceId(this.workspaceId).stream()
                .filter(this.workingCopyService::isAttached)
                .map(this::toProjectItem)
                .collect(toComposableHolder());

        this.dataProvider.getItems().addAll(items.getAll());
        this.dataProvider.refreshAll();
    }

    private void onPatchOperationProgress(PatchOperationType operationType, ProjectOperationProgress<? extends PatchOperationResult> progress) {
        this.projectProgressBar.update(progress);

        if (progress.getState() == State.FAILED) {
            var detailType = switch (operationType) {
                // TODO [Patching] make this obsolete
                case APPLY -> PatchOperationResultDetail.Type.APPLY_FAILED;
                case PREVIEW -> PatchOperationResultDetail.Type.PREVIEW_FAILED;
                case STOP -> PatchOperationResultDetail.Type.STOP_FAILED;
            };
            statisticsComponent.update(progress, detailType);
        }

        ProjectPatchItem item = this.items.get(progress.getFQPN());
        if (progress.getState() == State.DONE) {
            progress.getResult().ifPresent(r -> statisticsComponent.update(progress, r.getDetail().getType()));
            this.grid.setDetailsVisible(item, true);
        }
        item.replaceTrait(HasOperationProgress.class,
                c -> c.toBuilder().operationProgress(progress).build());

        this.dataProvider.refreshAll();
        this.dataProvider.refreshItem(item);
    }

    private ProjectPatchItem toProjectItem(Project project) {
        WorkingCopy workingCopy = this.workingCopyService.require(project);
        return ProjectPatchItem.builder()
                .build()
                .addTrait(HasProject.class, () -> project)
                .addTrait(HasIcon.class, HasIcon.builder()
                        .icon(this.imageResolverService.getProjectImage(project).orElse(null))
                        .build())
                .addTrait(HasWorkingCopy.class, HasWorkingCopy.builder().workingCopy(workingCopy).build())
                .addTrait(HasOperationProgress.class, HasOperationProgress.empty());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.workspaceId = event.getRouteParameters().get("workspaceId").orElseThrow();
    }
}

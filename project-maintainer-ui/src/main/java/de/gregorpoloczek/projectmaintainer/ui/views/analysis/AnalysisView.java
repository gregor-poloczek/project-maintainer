package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import static de.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder.toComposableHolder;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import de.gregorpoloczek.projectmaintainer.analysis.service.label.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.service.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.ui.common.progress.ProjectProgressBar;
import de.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasLabelsFilterComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasProjectFilterComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.IconComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.LabelsComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectNameComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasLabels;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Route(layout = MainLayout.class)
public class AnalysisView extends VerticalLayout {

    private final ProjectAnalysisService projectAnalysisService;
    private final ProjectService projectService;
    private final WorkingCopyService workingCopyService;
    private final LabelService labelService;
    private final Grid<ProjectAnalysisItem> grid;
    private final ListDataProvider<ProjectAnalysisItem> dataProvider = new ListDataProvider<>(new ArrayList<>());
    private final ProjectProgressBar projectProgressBar;

    private ComposableHolder<FQPN, ProjectAnalysisItem> items;
    private final ImageResolverService imageResolverService;
    private final transient Disposable.Swap currentOperation = Disposables.swap();

    public AnalysisView(
            ProjectAnalysisService projectAnalysisService,
            ProjectService projectService,
            WorkingCopyService workingCopyService,
            LabelService labelService,
            ImageResolverService imageResolverService
    ) {
        this.projectAnalysisService = projectAnalysisService;
        this.projectService = projectService;
        this.workingCopyService = workingCopyService;
        this.labelService = labelService;
        this.imageResolverService = imageResolverService;

        this.projectProgressBar = new ProjectProgressBar();
        this.projectProgressBar.setWidthFull();

        ComposableFilterSearch<ProjectAnalysisItem> search = new ComposableFilterSearch<>(this.dataProvider);
        this.grid = new Grid<>();
        this.grid.setDataProvider(dataProvider);

        HasLabelsFilterComponent hasLabelsFilterComponent = new HasLabelsFilterComponent(search);

        this.grid.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(ProjectNameComponent.getRenderer()).setHeader("Name").setFlexGrow(0).setWidth("350px");
        this.grid.addColumn(LabelsComponent.getRenderer(hasLabelsFilterComponent::getValue))
                .setHeader("Labels");
        this.add(
                new HorizontalLayout(
                        new HasProjectFilterComponent<>(search),
                        hasLabelsFilterComponent), grid, projectProgressBar);
        this.setSizeFull();
        this.grid.setSizeFull();

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        List<Project> projects = this.projectService.findAll();

        this.items = projects.stream()
                .filter(workingCopyService::hasWorkspace)
                .map(this::toItem).collect(toComposableHolder());
        this.dataProvider.getItems().clear();
        this.dataProvider.getItems().addAll(items.getAll());
        this.dataProvider.refreshAll();

        this.projectProgressBar.start(this.items.getAll(), "Analyzing projects ...");

        UI ui = UI.getCurrent();
        Disposable disposable = Flux.merge(items
                        .stream()
                        .map(p -> projectAnalysisService.analyze(p)
                                .subscribeOn(Schedulers.parallel()))
                        .toList())
                .doFinally(_ -> VaadinUtils.access(this.projectProgressBar, ProjectProgressBar::stop))
                .subscribe(progress -> this.onUpdateEvent(progress, ui));

        currentOperation.update(disposable);
    }

    private ProjectAnalysisItem toItem(Project project) {
        Optional<Image> icon = this.imageResolverService.getProjectImage(project);
        return ProjectAnalysisItem.builder()
                .fqpn(project.getFQPN())
                .build()
                .addTrait(HasProject.class, () -> project)
                .addTrait(HasLabels.class, new HasLabels(Collections.emptyList()))
                .addTrait(HasIcon.class, HasIcon.builder().icon(icon.orElse(null)).build());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.currentOperation.dispose();
    }


    private void onUpdateEvent(ProjectOperationProgress<Void> e, UI ui) {
        ui.access(() -> {
            if (!ui.isAttached()) {
                return;
            }
            this.projectProgressBar.update(e);

            if (e.getState().isTerminated()) {
                SortedSet<Label> labels = this.labelService.find(e.getFQPN());
                ProjectAnalysisItem item = this.items.get(e.getFQPN());
                item.replaceTrait(HasLabels.class, l -> new HasLabels(labels));
                this.dataProvider.refreshItem(item);
            }
        });
    }

}

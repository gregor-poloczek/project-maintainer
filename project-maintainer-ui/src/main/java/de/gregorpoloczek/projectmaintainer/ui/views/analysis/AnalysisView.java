package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import de.gregorpoloczek.projectmaintainer.analysis.service.label.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.service.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
    private Map<FQPN, ProjectAnalysisItem> itemByFQPN = new HashMap<>();
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

        ComposableFilterSearch<ProjectAnalysisItem> search = new ComposableFilterSearch<>(this.dataProvider);
        this.grid = new Grid<>();
        this.grid.setDataProvider(dataProvider);

        TextField labelsSearchFilter = new TextField();
        labelsSearchFilter.setPlaceholder("Labels");
        labelsSearchFilter.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        labelsSearchFilter.setValueChangeMode(ValueChangeMode.EAGER);
        var handle = search.add(
                c -> StringUtils.isBlank(labelsSearchFilter.getValue()) || c.requireTrait(HasLabels.class)
                        .getLabels()
                        .stream()
                        .anyMatch(l -> l.getValue()
                                .toLowerCase()
                                .contains(labelsSearchFilter.getValue().toLowerCase())));
        labelsSearchFilter.addValueChangeListener(_ -> handle.refresh());

        this.grid.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(ProjectNameComponent.getRenderer()).setHeader("Name").setFlexGrow(0).setWidth("350px");
        this.grid.addColumn(LabelsComponent.getRenderer(labelsSearchFilter::getValue))
                .setHeader("Labels");

        HorizontalLayout filters = new HorizontalLayout(new HasProjectFilterComponent<>(search),
                labelsSearchFilter);

        this.add(filters, grid);
        this.setSizeFull();
        this.grid.setSizeFull();

    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<Project> projects = this.projectService.findALl();
        UI ui = UI.getCurrent();
        List<ProjectAnalysisItem> items = new ArrayList<>();
        for (Project project : projects) {
            Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project);

            if (workingCopy.isPresent()) {
                Optional<Image> icon = AnalysisView.this.imageResolverService.getProjectImage(project);
                items.add(ProjectAnalysisItem.builder()
                        .build()
                        .addTrait(HasProject.class, () -> project)
                        .addTrait(HasLabels.class, new HasLabels(Collections.emptyList()))
                        .addTrait(HasIcon.class, HasIcon.builder().icon(icon.orElse(null)).build())
                );
            }
        }
        this.dataProvider.getItems().clear();
        this.dataProvider.getItems().addAll(items);
        this.dataProvider.refreshAll();
        this.itemByFQPN = items.stream().collect(toMap(ProjectAnalysisItem::getKey, identity()));

        Disposable disposable = Flux.merge(projectService.findALl()
                        .stream()
                        .filter(p -> workingCopyService.find(p.getMetaData().getFQPN()).isPresent())
                        .map(p -> projectAnalysisService.analyze(p.getMetaData().getFQPN())
                                .subscribeOn(Schedulers.parallel())
                                .last())
                        .toList())
                .subscribe(progress -> this.onUpdateEvent(progress, ui));

        currentOperation.update(disposable);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.currentOperation.dispose();
    }


    private void onUpdateEvent(ProjectOperationProgress<Void> e, UI ui) {
        if (!ui.isAttached()) {
            // browser has been reloaded or closed in the mean time
            return;
        }
        ui.access(() -> {
            if (e.getState().isTerminated()) {
                SortedSet<Label> labels = this.labelService.find(e.getFQPN());
                ProjectAnalysisItem item = this.itemByFQPN.get(e.getFQPN());
                item.replaceTrait(HasLabels.class, l -> new HasLabels(labels));
                this.grid.getDataProvider().refreshItem(item);
            }
        });
    }

}

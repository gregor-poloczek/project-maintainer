package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import static java.util.function.Function.identity;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
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
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Route(layout = MainLayout.class)
public class AnalysisView extends VerticalLayout {

    private final ProjectAnalysisService projectAnalysisService;
    private final ProjectService projectService;
    private final WorkingCopyService workingCopyService;
    private final LabelService labelService;
    private final Text text;
    private final TextField search;
    private final Grid<ProjectAnalysis> grid;
    private Map<FQPN, ProjectAnalysis> itemByFQPN = new HashMap<>();
    private ImageResolverService imageResolverService;

    @SuppressWarnings("unchecked")
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
        text = new Text("asd");

        this.grid = new Grid<>();

        search = new TextField();

        this.grid.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(ProjectNameComponent.getRenderer()).setHeader("Name").setFlexGrow(0).setWidth("350px");
        this.grid.addColumn(LabelsComponent.getRenderer(search::getValue))
                .setHeader("Labels");
        search.setPlaceholder("Search");
        search.setValueChangeMode(ValueChangeMode.TIMEOUT);
        search.setValueChangeTimeout(500);
        search.addValueChangeListener(e -> {
            ListDataProvider<ProjectAnalysis> dataProvider = (ListDataProvider<ProjectAnalysis>) this.grid.getDataProvider();
            String query = e.getValue().toLowerCase();
            dataProvider.setFilter(
                    i -> StringUtils.isBlank(query) || i.matches(query));
        });

        this.add(search, grid);
        this.setSizeFull();
        this.grid.setSizeFull();

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<Project> projects = this.projectService.findALl();
        UI ui = UI.getCurrent();
        List<ProjectAnalysis> items = new ArrayList<>();
        for (Project project : projects) {
            Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project);

            if (workingCopy.isPresent()) {
                Optional<Image> icon = AnalysisView.this.imageResolverService.getProjectImage(project);
                items.add(ProjectAnalysis.builder()
                        .build()
                        .addTrait(HasProject.class, () -> project)
                        .addTrait(HasLabels.class, new HasLabels(Collections.emptyList()))
                        .addTrait(HasIcon.class, HasIcon.builder().icon(icon.orElse(null)).build())
                );
            }
        }
        this.grid.setItems(items);
        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(
                        p -> p.requireTrait(HasProject.class).getProject().getMetaData().getFQPN(),
                        identity()));

        Flux.merge(projectService.findALl()
                .stream()
                .filter(p -> workingCopyService.find(p.getMetaData().getFQPN()).isPresent())
                .map(p -> projectAnalysisService.analyze(p.getMetaData().getFQPN())
                        .subscribeOn(Schedulers.parallel())
                        .last())
                .toList()).subscribe(progress -> {
            this.onUpdateEvent(progress, ui);
        });
    }

    private void onUpdateEvent(ProjectOperationProgress<Void> e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the mean time
            return;
        }
        current.access(() -> {
            if (e.getState().isTerminated()) {
                SortedSet<Label> labels = this.labelService.find(e.getFQPN());
                ProjectAnalysis item = this.itemByFQPN.get(e.getFQPN());
                item.replaceTrait(HasLabels.class, l -> new HasLabels(labels));
                this.grid.getDataProvider().refreshItem(item);
            }
            // this.text.setText("Analysis progress: " + e.getProgress());
        });
    }

}

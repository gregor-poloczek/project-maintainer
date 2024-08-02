package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.git.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.git.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Function;
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
    private final Grid<ProjectAnalysisItem> grid;
    private Map<FQPN, ProjectAnalysisItem> itemByFQPN = new HashMap<>();
    private ImageResolverService imageResolverService;

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

        this.grid.addColumn(Renderers.getIconRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(Renderers.getNameRenderer()).setHeader("Name").setFlexGrow(0).setWidth("350px");
        this.grid.addColumn(Renderers.getLabelsRenderer(search::getValue))
                .setHeader("Labels");
        search.setPlaceholder("Search");
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> {
            ListDataProvider<ProjectAnalysisItem> dataProvider = (ListDataProvider<ProjectAnalysisItem>) this.grid.getDataProvider();
            String query = e.getValue().toLowerCase();
            dataProvider.setFilter(
                    i -> StringUtils.isBlank(query) || i.matches(query));
            // TODO brauch ich das?
            dataProvider.refreshAll();
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
        List<ProjectAnalysisItem> items = new ArrayList<>();
        for (Project project : projects) {
            Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project);

            if (workingCopy.isPresent()) {
                items.add(ProjectAnalysisItem.builder()
                        .project(project)
                        .icon(AnalysisView.this.imageResolverService.getProjectImage(project))
                        .build());
            }
        }
        this.grid.setItems(items);
        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(p -> p.getProject().getMetaData().getFQPN(), Function.identity()));

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
                ProjectAnalysisItem item = this.itemByFQPN.get(e.getFQPN());
                item.setLabels(labels);
                this.grid.getDataProvider().refreshItem(item);
            }
            // this.text.setText("Analysis progress: " + e.getProgress());
        });
    }

}

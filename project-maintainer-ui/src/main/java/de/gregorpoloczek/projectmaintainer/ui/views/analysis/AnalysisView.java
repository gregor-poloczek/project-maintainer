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
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.LabelService;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
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

@Route
public class AnalysisView extends VerticalLayout {

    private final ProjectAnalysisService projectAnalysisService;
    private final ProjectService projectService;
    private final WorkingCopyService workingCopyService;
    private final OperationExecutionService operationExecutionService;
    private final LabelService labelService;
    private final Text text;
    private final TextField search;
    private final Grid<ProjectAnalysisItem> grid;
    private Map<FQPN, ProjectAnalysisItem> itemByFQPN = new HashMap<>();

    public AnalysisView(
            ProjectAnalysisService projectAnalysisService,
            ProjectService projectService,
            WorkingCopyService workingCopyService,
            OperationExecutionService operationExecutionService,
            LabelService labelService) {
        this.projectAnalysisService = projectAnalysisService;
        this.projectService = projectService;
        this.workingCopyService = workingCopyService;
        this.operationExecutionService = operationExecutionService;
        this.labelService = labelService;
        text = new Text("asd");

        this.grid = new Grid<>();
        this.grid.addColumn(Renderers.getNameRenderer()).setHeader("Name");
        this.grid.addColumn(i -> i.getLabels().stream().map(Label::getValue)
                        .collect(Collectors.joining(",")))
                .setHeader("Labels");

        search = new TextField();
        search.setPlaceholder("Search");
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> {
            ListDataProvider<ProjectAnalysisItem> dataProvider = (ListDataProvider<ProjectAnalysisItem>) this.grid.getDataProvider();
            String query = e.getValue();
            // TODO label matching with regexp
            dataProvider.setFilter(
                    i -> StringUtils.isBlank(query) || i.getName().toLowerCase().contains(query.toLowerCase()));
            // TODO brauch ich das?
            dataProvider.refreshAll();
        });

        this.add(search, grid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<Project> projects = this.projectService.getProjects();
        UI ui = UI.getCurrent();
        List<ProjectAnalysisItem> items = new ArrayList<>();
        for (Project project : projects) {
            Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project.getMetaData().getFQPN());

            if (workingCopy.isPresent()) {
                items.add(ProjectAnalysisItem.builder().project(project).build());
                this.operationExecutionService.executeAsyncOperation2(
                                project,
                                "analysis::analyze",
                                this.projectAnalysisService::analyze)
                        .subscribe(e -> onUpdateEvent(e, ui));
            }
        }
        this.grid.setItems(items);
        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(p -> p.getProject().getMetaData().getFQPN(), Function.identity()));
    }

    private void onUpdateEvent(ProjectOperationProgress e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the mean time
            return;
        }
        current.access(() -> {
            if (e.getState().isTerminated()) {
                SortedSet<Label> labels = this.labelService.find(e.getFqpn());
                ProjectAnalysisItem item = this.itemByFQPN.get(e.getFqpn());
                item.setLabels(labels);
                this.grid.getDataProvider().refreshItem(item);
            }
            this.text.setText("Analysis progress: " + e.getProgress());
        });
    }

}

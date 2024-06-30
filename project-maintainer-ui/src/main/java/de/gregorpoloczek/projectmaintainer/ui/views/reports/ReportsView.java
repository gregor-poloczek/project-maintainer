package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.analysis.VersionedLabel;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import de.gregorpoloczek.projectmaintainer.ui.views.reports.ReportingProperties.Column;
import de.gregorpoloczek.projectmaintainer.ui.views.reports.ReportingProperties.Report;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;

@Route("/reports/:reportId")
public class ReportsView extends VerticalLayout implements BeforeEnterObserver {

    private final H1 title;
    private final ReportingProperties reportingProperties;
    private final ProjectService projectService;
    private final Grid<ReportRowItem> grid;
    private WorkingCopyService workingCopyService;
    private OperationExecutionService operationExecutionService;
    private ProjectAnalysisService projectAnalysisService;
    private LabelService labelService;
    private Report report;

    public ReportsView(
            ReportingProperties reportingProperties,
            ProjectService projectService,
            WorkingCopyService workingCopyService, OperationExecutionService operationExecutionService,
            ProjectAnalysisService projectAnalysisService,
            LabelService labelService) {
        this.reportingProperties = reportingProperties;
        this.projectService = projectService;
        this.workingCopyService = workingCopyService;
        this.operationExecutionService = operationExecutionService;
        this.projectAnalysisService = projectAnalysisService;
        this.labelService = labelService;
        this.title = new H1("");

        this.grid = new Grid<>();

        this.grid.addColumn(Renderers.getIconRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(Renderers.getNameRenderer()).setHeader("Name").setFlexGrow(0).setWidth("350px");

        this.add(this.title, this.grid);
        this.setSizeFull();
        this.grid.setSizeFull();
    }


    @Builder
    public static class ReportRowItemCell {

        int index;

        public String getValue(ReportRowItem item) {
            return item.getValue(index).orElse("");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String reportId = parameters.get("reportId")
                .orElseThrow(() -> new IllegalArgumentException("No report id defined"));

        this.report = reportingProperties.getReports().stream()
                .filter(r -> r.getId().equals(reportId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report " + reportId + " is unknown."));

        int index = 0;
        for (Column columnId : report.getColumns()) {
            ReportRowItemCell cell = ReportRowItemCell.builder().index(index).build();
            this.grid.addColumn(cell::getValue)
                    .setHeader(columnId.getName());
            index++;
        }
        this.title.setText(report.getName());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {

        List<Project> projects = this.projectService.getProjects();
        UI ui = UI.getCurrent();
        List<ReportRowItem> items = new ArrayList<>();
        for (Project project : projects) {
            Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project.getMetaData().getFQPN());

            if (workingCopy.isPresent()) {
                items.add(new ReportRowItem(project, this.report.getColumns().size()));
                this.operationExecutionService.executeAsyncOperation2(
                                project,
                                "analysis::analyze",
                                this.projectAnalysisService::analyze)
                        .subscribe(e -> onUpdateEvent(e, ui));
            }
        }
        this.grid.setItems(items);
        ListDataProvider<ReportRowItem> dataProvider = (ListDataProvider<ReportRowItem>) this.grid.getDataProvider();
        dataProvider.setFilter(ReportRowItem::hasValues);
        dataProvider.refreshAll();

        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(p -> p.getProject().getMetaData().getFQPN(), Function.identity()));
    }

    private Map<FQPN, ReportRowItem> itemByFQPN = new HashMap<>();

    private void onUpdateEvent(ProjectOperationProgress e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the mean time
            return;
        }
        current.access(() -> {
            if (e.getState().isTerminated()) {
                SortedSet<Label> labels = this.labelService.find(e.getFqpn());
                ReportRowItem item = this.itemByFQPN.get(e.getFqpn());

                int index = 0;
                for (Column column : this.report.getColumns()) {
                    Label label = Label.of(column.getDependencyVersion());
                    Optional<VersionedLabel> match = labels.stream()
                            .filter(VersionedLabel.class::isInstance)
                            .map(VersionedLabel.class::cast)
                            .filter(vL -> vL.getBase().equals(label))
                            .findFirst();

                    if (match.isPresent()) {
                        item.setValue(index, match.get().getVersion());
                        ListDataProvider<ReportRowItem> dataProvider = (ListDataProvider<ReportRowItem>) this.grid.getDataProvider();
                        dataProvider.refreshItem(item);
                    }

                    index++;
                }

                this.grid.getDataProvider().refreshItem(item);
            }
        });
    }


}

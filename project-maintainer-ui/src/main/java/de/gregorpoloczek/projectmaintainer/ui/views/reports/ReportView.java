package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
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
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService.Cell;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService.Column;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService.Row;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ColumnProperties;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ReportProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;

@Route(value = "/reports/:reportId", layout = MainLayout.class)
public class ReportView extends VerticalLayout implements BeforeEnterObserver {

    private final transient ReportingProperties reportingProperties;
    private final transient ProjectService projectService;
    private final Grid<ReportRowItem> grid;
    private final ReportHeader header;
    private final transient WorkingCopyService workingCopyService;
    private final transient OperationExecutionService operationExecutionService;
    private final transient ProjectAnalysisService projectAnalysisService;
    private final transient LabelService labelService;
    private final ProjectReportGeneratorService projectReportGeneratorService;
    private final transient ImageResolverService imageResolverService;
    private transient ReportProperties reportProperties;
    private transient Map<FQPN, ReportRowItem> itemByFQPN = new HashMap<>();


    public ReportView(
            ReportingProperties reportingProperties,
            ProjectService projectService,
            WorkingCopyService workingCopyService, OperationExecutionService operationExecutionService,
            ProjectAnalysisService projectAnalysisService,
            LabelService labelService,
            ProjectReportGeneratorService projectReportGeneratorService,
            ImageResolverService imageResolverService) {
        this.reportingProperties = reportingProperties;
        this.projectService = projectService;
        this.workingCopyService = workingCopyService;
        this.operationExecutionService = operationExecutionService;
        this.projectAnalysisService = projectAnalysisService;
        this.labelService = labelService;
        this.projectReportGeneratorService = projectReportGeneratorService;
        this.imageResolverService = imageResolverService;
        this.header = new ReportHeader(reportingProperties);

        this.grid = new Grid<>();

        this.add(header, this.grid);
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

        this.reportProperties = reportingProperties.getReports().stream()
                .filter(r -> r.getId().equals(reportId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report " + reportId + " is unknown."));

        this.header.setTitle(reportProperties.getName());
        this.header.setSelectedReport(reportProperties);

        this.grid.removeAllColumns();
        this.grid.addColumn(Renderers.getIconRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(Renderers.getNameRenderer()).setHeader("Name").setFlexGrow(1).setWidth("350px");

        List<Project> projects = this.projectService.getProjects();
        List<ReportRowItem> items = new ArrayList<>();
        for (Project project : projects) {
            Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project.getMetaData().getFQPN());

            if (workingCopy.isPresent()) {
                items.add(
                        new ReportRowItem(project,
                                this.reportProperties.getColumns().size(),
                                ReportView.this.imageResolverService.getImage("gitprovider",
                                        project.getMetaData().getGitProvider().name()))
                );
            }
        }

        synchronized (this) {
            this.itemByFQPN = items.stream()
                    .collect(Collectors.toMap(p -> p.getProject().getMetaData().getFQPN(), Function.identity()));
            this.grid.setItems(items);
            ListDataProvider<ReportRowItem> dataProvider = (ListDataProvider<ReportRowItem>) this.grid.getDataProvider();
            dataProvider.setFilter(ReportRowItem::hasValues);
            dataProvider.refreshAll();
        }

        UI ui = UI.getCurrent();

        projectReportGeneratorService.getReport(reportId)
                .subscribe((report) -> ui.access(() -> {
                    int index = 0;
                    for (Column column : report.getDefinition().getColumns()) {
                        ReportRowItemCell cell = ReportRowItemCell.builder().index(index).build();
                        this.grid.addColumn(cell::getValue)
                                .setHeader(column.getLabel()).setWidth("128px")
                                .setFlexGrow(0)
                                .setTextAlign(ColumnTextAlign.CENTER);
                        index++;
                    }

                    for (Row row : report.getRows()) {
                        ReportRowItem item = this.itemByFQPN.get(row.getProject().getMetaData().getFQPN());
                        int i = 0;
                        for (Cell cell : row.getCells()) {
                            item.setValue(i, cell.getValue() != null ? cell.getValue() : "");
                            i++;
                        }
                        grid.getDataProvider().refreshAll();
                    }
                }));

    }

//    private void onUpdateEvent(ProjectOperationProgress e, UI current) {
//        if (!current.isAttached()) {
//            // browser has been reloaded or closed in the mean time
//            return;
//        }
//        current.access(() -> {
//            if (e.getState().isTerminated()) {
//                FQPN fqpn = e.getFqpn();
//                synchronized (this) {
//                    ReportRowItem item = this.itemByFQPN.get(fqpn);
//                    this.fillRowItem(fqpn, item);
//                    this.grid.getDataProvider().refreshAll();
//                }
//            }
//        });
//    }

//    private void fillRowItem(FQPN fqpn, ReportRowItem item) {
//        SortedSet<Label> labels = this.labelService.find(fqpn);
//        int index = 0;
//        for (ColumnProperties column : this.reportProperties.getColumns()) {
//            Label label = Label.of(column.getVersionLabelBase());
//            Optional<VersionedLabel> match = labels.stream()
//                    .filter(VersionedLabel.class::isInstance)
//                    .map(VersionedLabel.class::cast)
//                    .filter(vL -> vL.getBase().equals(label))
//                    .findFirst();
//
//            if (match.isPresent()) {
//                item.setValue(index, match.get().getVersion());
//            }
//            index++;
//        }
//    }


}

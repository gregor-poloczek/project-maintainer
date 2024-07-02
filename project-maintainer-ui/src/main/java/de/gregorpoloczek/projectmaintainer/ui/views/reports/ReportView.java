package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService.Cell;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService.Column;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService.Report;
import de.gregorpoloczek.projectmaintainer.reporting.ProjectReportGeneratorService.Row;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ReportProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

@Route(value = "/reports/:reportId", layout = MainLayout.class)
@Slf4j
public class ReportView extends VerticalLayout implements BeforeEnterObserver {

    private final transient ReportingProperties reportingProperties;
    private final Grid<ReportRowItem> grid;
    private final ReportHeader header;
    private final ProjectReportGeneratorService projectReportGeneratorService;
    private final transient ImageResolverService imageResolverService;
    private transient ReportProperties reportProperties;


    public ReportView(
            ReportingProperties reportingProperties,
            ProjectReportGeneratorService projectReportGeneratorService,
            ImageResolverService imageResolverService) {
        this.reportingProperties = reportingProperties;
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

        UI ui = UI.getCurrent();
        projectReportGeneratorService.getReport(reportId)
                .subscribeOn(Schedulers.parallel())
                .subscribe(report -> ui.access(() -> applyReport(report)));
    }

    private void applyReport(Report report) {
        int index = 0;
        for (Column column : report.getDefinition().getColumns()) {
            ReportRowItemCell cell = ReportRowItemCell.builder().index(index).build();
            this.grid.addColumn(cell::getValue)
                    .setHeader(column.getLabel()).setWidth("128px")
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
            index++;
        }

        List<ReportRowItem> items = new ArrayList<>();
        for (Row row : report.getRows()) {
            Project project = row.getProject();
            ReportRowItem item = new ReportRowItem(project,
                    this.reportProperties.getColumns().size(),
                    ReportView.this.imageResolverService.getImage("gitprovider",
                            project.getMetaData().getGitProvider().name()));
            int i = 0;
            for (Cell cell : row.getCells()) {
                item.setValue(i, cell.getValue() != null ? cell.getValue() : "");
                i++;
            }
            items.add(item);
        }
        grid.setItems(items);
    }


}

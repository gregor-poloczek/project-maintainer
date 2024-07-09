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
import de.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService;
import de.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService.ProjectReportGenerationProgress.State;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportCell;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportColumn;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReport;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportRow;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

@Route(value = "/reports/:reportId", layout = MainLayout.class)
@Slf4j
public class ReportView extends VerticalLayout implements BeforeEnterObserver {

    private final Grid<ReportRowItem> grid;
    private final ReportHeader header;
    private final ReportGeneratorService projectReportGeneratorService;
    private final transient ImageResolverService imageResolverService;
    private transient ProjectReportConfig reportConfig;


    public ReportView(
            ReportGeneratorService reportGeneratorService,
            ImageResolverService imageResolverService) {
        this.projectReportGeneratorService = reportGeneratorService;
        this.imageResolverService = imageResolverService;
        this.header = new ReportHeader(reportGeneratorService.getProjectReportConfigs());

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

        this.reportConfig = projectReportGeneratorService.getProjectReportConfigs().stream()
                .filter(r -> r.getId().equals(reportId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report " + reportId + " is unknown."));

        this.header.setTitle(reportConfig.getName());
        this.header.setSelectedReport(reportConfig);

        this.grid.removeAllColumns();
        this.grid.addColumn(Renderers.getIconRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(Renderers.getNameRenderer()).setHeader("Project").setFlexGrow(1).setWidth("350px");

        UI ui = UI.getCurrent();

        // TODO cancel subscription on route change
        // TODO error handling
        projectReportGeneratorService.generateProjectReport(reportId)
                .subscribeOn(Schedulers.parallel())
                .subscribe(progress ->
                        ui.access(() -> {
                            this.header.updateProgress(progress.getProgressCurrent(), progress.getProgressTotal());
                            if (progress.getState() == State.DONE) {
                                applyReport(progress.getProjectReport());
                            }
                        })
                );
    }

    private void applyReport(ProjectReport report) {
        int index = 0;
        for (ProjectReportColumn column : report.getDefinition().getColumns()) {
            ReportRowItemCell cell = ReportRowItemCell.builder().index(index).build();
            this.grid.addColumn(cell::getValue)
                    .setHeader(column.getLabel()).setWidth("128px")
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
            index++;
        }

        List<ReportRowItem> items = new ArrayList<>();
        for (ProjectReportRow row : report.getRows()) {
            Project project = row.getProject();

            Optional<Image> image = imageResolverService.getProjectImage(row.getProject());

            ReportRowItem item = new ReportRowItem(project,
                    this.reportConfig.getColumns().size(), image);

            int i = 0;
            for (ProjectReportCell cell : row.getCells()) {
                item.setValue(i, cell.getValue() != null ? cell.getValue() : "");
                i++;
            }
            items.add(item);
        }
        grid.setItems(items);
    }


}

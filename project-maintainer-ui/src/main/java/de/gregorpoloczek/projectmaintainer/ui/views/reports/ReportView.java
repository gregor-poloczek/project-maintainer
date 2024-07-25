package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellHrefValue;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportGenerationProgress.State;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportCell;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportColumn;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReport;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportDefinition;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportRow;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Schedulers;

@Route(value = "/reports/:reportId", layout = MainLayout.class)
@Slf4j
public class ReportView extends VerticalLayout implements BeforeEnterObserver {

    private final Grid<ReportRowItem> grid;
    private final ReportHeader header;
    private final transient ReportGeneratorService projectReportGeneratorService;
    private final transient ImageResolverService imageResolverService;
    private final transient Disposable.Swap currentGeneration = Disposables.swap();
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
        this.grid.addColumn(Renderers.getNameRenderer()).setHeader("Project").setFlexGrow(2).setWidth("350px");

        UI ui = UI.getCurrent();

        // TODO error handling
        this.currentGeneration.update(projectReportGeneratorService.generateProjectReport(reportId)
                .subscribeOn(Schedulers.parallel())
                .subscribe(progress ->
                        ui.access(() -> {
                            if (progress.getState() == State.SCHEDULED) {
                                this.applyReportDefinition(progress.getProjectReport().getDefinition());
                                this.header.updateProgress(progress.getProgressCurrent(), progress.getProgressTotal());
                            }
                            if (progress.getState() == State.RUNNING) {
                                // TODO report can be concurrently modified
                                applyReport(progress.getProjectReport());
                                this.header.updateProgress(progress.getProgressCurrent(), progress.getProgressTotal());
                            }
                            if (progress.getState() == State.DONE) {
                                applyReport(progress.getProjectReport());
                            }
                            if (progress.getState().isTerminated()) {
                                this.header.hideProgress();
                            }
                        })
                ));
    }

    private void applyReportDefinition(ProjectReportDefinition definition) {
        this.grid.setItems(new ArrayList<>());
        int index = 0;
        for (ReportColumn column : definition.getColumns()) {
            final int i = index;
            this.grid
                    .addColumn(createCellRenderer(i))
                    .setHeader(column.getLabel())
                    .setFlexGrow(1)
                    // .setWidth("250px")
                    .setTextAlign(
                            toTextAlign(column.getTextAlignment()));
            index++;
        }
    }

    private static ComponentRenderer<HorizontalLayout, ReportRowItem> createCellRenderer(
            int columnIndex) {
        return new ComponentRenderer<>(
                (ReportRowItem item) -> {
                    HorizontalLayout result = new HorizontalLayout();
                    Optional<ReportCellValue> maybeValue = item.getValue(columnIndex);

                    if (maybeValue.isPresent()) {
                        ReportCellValue value = maybeValue.get();

                        Component c = switch (value) {
                            case ReportCellHrefValue href -> {
                                Anchor anchor = new Anchor(href.getHref(), href.getText());
                                anchor.setTarget("_blank");
                                yield anchor;
                            }
                            default -> new Span(value.getStringValue());
                        };
                        result.add(c);
                    }
                    return result;
                });
    }

    private ColumnTextAlign toTextAlign(ColumnTextAlignment textAlignment) {
        return switch (textAlignment) {
            case LEFT -> ColumnTextAlign.START;
            case CENTER -> ColumnTextAlign.CENTER;
            case RIGHT -> ColumnTextAlign.END;
        };
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.currentGeneration.dispose();
    }

    private void applyReport(ProjectReport report) {
        List<ReportRowItem> items = new ArrayList<>();
        for (ProjectReportRow row : report.getRows()) {
            Project project = row.getProject();

            Optional<Image> image = imageResolverService.getProjectImage(row.getProject());

            ReportRowItem item = new ReportRowItem(project,
                    this.reportConfig.getColumns().size(), image);

            int i = 0;
            for (ProjectReportCell cell : row.getCells()) {
                item.setValue(i, cell.getValue());
                i++;
            }
            items.add(item);
        }
        grid.setItems(items);
        // TODO only add items that are missing, instead of replacing all of them
    }


}

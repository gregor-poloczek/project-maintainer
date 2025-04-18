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
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellBooleanValue;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportCell;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportColumn;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReport;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportDefinition;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportRow;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasProjectFilterComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.IconComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectNameComponent;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Schedulers;

@Route(value = "/reports/:reportId", layout = MainLayout.class)
@Slf4j
public class ReportView extends VerticalLayout implements BeforeEnterObserver {

    private final Grid<ReportRow> grid;
    private final ReportHeader header;
    private final transient ReportGeneratorService projectReportGeneratorService;
    private final transient ImageResolverService imageResolverService;
    private final transient Disposable.Swap currentOperation = Disposables.swap();
    private transient ProjectReportConfig reportConfig;
    private final transient ListDataProvider<ReportRow> dataProvider = new ListDataProvider<>(new ArrayList<>());
    private final ComposableHolder<FQPN, ReportRow> rows = ComposableHolder.emptyHolder();

    public ReportView(
            ReportGeneratorService reportGeneratorService,
            ImageResolverService imageResolverService) {
        this.projectReportGeneratorService = reportGeneratorService;
        this.imageResolverService = imageResolverService;
        this.header = new ReportHeader(reportGeneratorService.getProjectReportConfigs());

        ComposableFilterSearch<ReportRow> search = new ComposableFilterSearch<>(this.dataProvider);
        this.grid = new Grid<>();
        this.grid.setDataProvider(dataProvider);

        this.add(header, new HasProjectFilterComponent<>(search), this.grid);
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
        this.grid.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(ProjectNameComponent.getRenderer()).setHeader("Project").setFlexGrow(2).setWidth("350px");

        UI ui = UI.getCurrent();

        // TODO error handling
        this.currentOperation.update(projectReportGeneratorService.generateProjectReport(reportId)
                .subscribeOn(Schedulers.parallel())
                .subscribe(progress ->
                        ui.access(() -> {
                            if (progress.getState() == OperationProgress.State.SCHEDULED) {
                                this.applyReportDefinition(progress.getResult().getDefinition());
                                this.header.updateProgress(progress.getProgressCurrent(), progress.getProgressTotal());

                                this.rows.clear();
                                dataProvider.getItems().clear();
                                dataProvider.refreshAll();
                            }
                            if (progress.getState() == OperationProgress.State.RUNNING) {
                                // TODO report can be concurrently modified
                                applyReport(progress.getResult());
                                this.header.updateProgress(progress.getProgressCurrent(), progress.getProgressTotal());
                            }
                            if (progress.getState() == OperationProgress.State.DONE) {
                                applyReport(progress.getResult());
                            }
                            if (progress.getState().isTerminated()) {
                                this.header.hideProgress();
                            }
                        })
                ));
    }

    private void applyReportDefinition(ProjectReportDefinition definition) {
        dataProvider.getItems().clear();
        dataProvider.refreshAll();

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

    private static ComponentRenderer<HorizontalLayout, ReportRow> createCellRenderer(
            int columnIndex) {
        return new ComponentRenderer<>(
                (ReportRow item) -> {
                    // TODO durch horizontal layout ist die text ausrichtung nun total broken
                    HorizontalLayout result = new HorizontalLayout();
                    Optional<ReportCellValue> maybeValue = item.getValue(columnIndex);

                    if (maybeValue.isPresent()) {
                        ReportCellValue value = maybeValue.get();

                        Component c = switch (value) {
                            case ReportCellBooleanValue booleanValue ->
                                    new Span(booleanValue.getBooleanValue() ? "✅" : "❌");
                            default -> new Span(value.getStringValue());
                        };
                        Project project = item.requireTrait(HasProject.class).getProject();
                        if (value.getLocation().isPresent() && project
                                .getMetaData()
                                .getBrowserLink()
                                .isPresent()) {
                            String href = project.getMetaData().getBrowserLink().get() + value.getLocation()
                                    .get()
                                    .getRelativePath()
                                    .toString();
                            Anchor anchor = new Anchor(href, c);
                            anchor.setTarget("_blank");
                            c = anchor;
                        }
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
        this.currentOperation.dispose();
    }

    private void applyReport(ProjectReport report) {
        for (ProjectReportRow row : report.getRows()) {
            Project project = row.getProject();

            Optional<Image> image = imageResolverService.getProjectImage(row.getProject());

            FQPN fqpn = row.getProject().getFQPN();
            ReportRow item = this.rows.compute(fqpn,
                    () -> new ReportRow(fqpn, this.reportConfig.getColumns().size())
                            .addTrait(HasProject.class, () -> project)
                            .addTrait(HasIcon.class, HasIcon.builder().icon(image.orElse(null)).build()));
            int i = 0;
            for (ProjectReportCell cell : row.getCells()) {
                item.setValue(i, cell.getValue());
                i++;
            }
        }

        dataProvider.getItems().clear();
        dataProvider.getItems()
                .addAll(this.rows.getAll().stream().sorted(Comparator.comparing(ReportRow::getKey)).toList());
        dataProvider.refreshAll();

        // TODO only add items that are missing, instead of replacing all of them
    }


}

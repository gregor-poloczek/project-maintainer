package io.github.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.ProjectFullTextSearchService;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService;
import io.github.gregorpoloczek.projectmaintainer.reporting.common.ReportCellBooleanValue;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportCell;
import io.github.gregorpoloczek.projectmaintainer.reporting.common.ReportColumn;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReport;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportDefinition;
import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportRow;
import io.github.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import io.github.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.ComposableHolder;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components.HasProjectFilterComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import io.github.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import io.github.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.IconComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.components.ProjectNameComponent;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasIcon;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import io.github.gregorpoloczek.projectmaintainer.ui.common.progress.LabeledProgressBar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Schedulers;

@Route(value = "/workspace/:workspaceId/reports/:reportId", layout = MainLayout.class)
@Slf4j
public class ReportView extends VerticalLayout implements BeforeEnterObserver {

    private final Grid<ReportRow> grid;
    private final ReportHeader header;
    private final transient ReportGeneratorService projectReportGeneratorService;
    private final transient ImageResolverService imageResolverService;
    private final transient Disposable.Swap currentOperation = Disposables.swap();
    private final ComposableFilterSearch<FQPN, ReportRow> search;
    private final ProjectFullTextSearchService projectFullTextSearchService;
    private final LabeledProgressBar progressBar;
    private transient ProjectReportConfig reportConfig;
    private final transient ListDataProvider<ReportRow> dataProvider = new ListDataProvider<>(new ArrayList<>());
    private final ComposableHolder<FQPN, ReportRow> rows = ComposableHolder.emptyHolder();
    private HeaderRow filterHeaderRow;
    private String workspaceId;

    public ReportView(
            ReportGeneratorService reportGeneratorService,
            ImageResolverService imageResolverService, ProjectFullTextSearchService projectFullTextSearchService) {
        this.projectReportGeneratorService = reportGeneratorService;
        this.imageResolverService = imageResolverService;
        this.header = new ReportHeader(this.workspaceId, reportGeneratorService.getProjectReportConfigs());

        this.search = new ComposableFilterSearch<>(this.dataProvider);
        this.progressBar = new LabeledProgressBar();
        this.progressBar.setWidthFull();
        this.progressBar.setVisible(false);
        this.grid = new Grid<>();
        this.grid.setDataProvider(dataProvider);

        this.add(header, this.grid, this.progressBar);
        this.setSizeFull();
        this.grid.setSizeFull();
        this.projectFullTextSearchService = projectFullTextSearchService;
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String reportId = parameters.get("reportId")
                .orElseThrow(() -> new IllegalArgumentException("No report id defined"));

        this.workspaceId = event.getRouteParameters().get("workspaceId").orElseThrow();


        this.reportConfig = projectReportGeneratorService.getProjectReportConfigs().stream()
                .filter(r -> r.getId().equals(reportId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report " + reportId + " is unknown."));

        this.header.setTitle(reportConfig.getName());
        this.header.setSelectedReport(reportConfig);

        this.grid.removeAllColumns();
        this.grid.addColumn(IconComponent.getRenderer()).setFlexGrow(0).setWidth("64px");
        this.grid.addColumn(ProjectNameComponent.getRenderer())
                .setHeader("Project")
                .setFlexGrow(2)
                .setWidth("350px")
                .setResizable(true);

        UI ui = UI.getCurrent();

        progressBar.setLabel("Generating report ...");
        progressBar.setValue(0.0d);
        progressBar.setVisible(true);

        // TODO [Reporting] error handling
        this.currentOperation.update(projectReportGeneratorService.generateProjectReport(this.workspaceId, reportId)
                .subscribeOn(Schedulers.parallel())
                .doFinally(s -> VaadinUtils.access(progressBar, p -> p.setVisible(false)))
                .subscribe(progress ->
                        ui.access(() -> {
                            this.progressBar.setValue(progress.getProgressRelative());
                            if (progress.getState() == OperationProgress.State.SCHEDULED) {
                                this.applyReportDefinition(progress.getResult().orElseThrow().getDefinition());
                                this.rows.clear();
                                dataProvider.getItems().clear();
                                dataProvider.refreshAll();
                            }
                            if (progress.getState() == OperationProgress.State.RUNNING) {
                                // TODO report can be concurrently modified
                                applyReport(progress.getResult().orElseThrow());
                            }
                            if (progress.getState() == OperationProgress.State.DONE) {
                                applyReport(progress.getResult().orElseThrow());
                            }
                        })
                ));
    }

    private void applyReportDefinition(ProjectReportDefinition definition) {
        this.rows.clear();
        this.dataProvider.getItems().clear();
        this.dataProvider.refreshAll();

        // create a new header row for filtering
        if (this.filterHeaderRow != null) {
            this.grid.removeHeaderRow(this.filterHeaderRow);
        }
        this.filterHeaderRow = grid.appendHeaderRow();
        // second column is always the project name
        HasProjectFilterComponent<FQPN, ReportRow> projectFilter = new HasProjectFilterComponent<>(search);
        projectFilter.setDecorated(false);
        this.filterHeaderRow.getCell(this.grid.getColumns().get(1)).setComponent(projectFilter);

        int index = 0;
        for (ReportColumn reportColumn : definition.getColumns()) {
            final int i = index;
            Column<ReportRow> column = this.grid
                    .addColumn(createCellRenderer(i))
                    .setHeader(reportColumn.getLabel())
                    .setFlexGrow(1)
                    .setResizable(true)
                    .setTextAlign(
                            toTextAlign(reportColumn.getTextAlignment()));

            // append a filtering component
            filterHeaderRow
                    .getCell(column)
                    .setComponent(createFilterComponent(index));

            index++;
        }

    }

    private Component createFilterComponent(int columnIndex) {
        TextField result = new TextField();
        result.setWidthFull();
        result.setValueChangeMode(ValueChangeMode.EAGER);
        var handler = search.add(row -> {
            Optional<String> maybeQuery = Optional.of(result.getValue())
                    .map(StringUtils::trim)
                    .filter(StringUtils::isNotBlank);
            if (maybeQuery.isEmpty()) {
                return true;
            }
            String query = maybeQuery.get();

            Optional<ReportCellValue> maybeValue = row.getValue(columnIndex);
            if (maybeValue.isEmpty()) {
                return false;
            }
            return maybeValue.get().getStringValue().toLowerCase().contains(query.toLowerCase());
        });
        result.addAttachListener(e1 -> result.addValueChangeListener(e2 -> handler.refresh()));
        result.addDetachListener(e -> handler.remove());
        return result;
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

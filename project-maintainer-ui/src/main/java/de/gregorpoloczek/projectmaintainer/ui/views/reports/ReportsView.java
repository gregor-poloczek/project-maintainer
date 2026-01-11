package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;

@Route(value = "/workspace/:workspaceId/reports", layout = MainLayout.class)
public class ReportsView extends VerticalLayout {

    public ReportsView(
            ReportGeneratorService reportGeneratorService) {
        ReportHeader header = new ReportHeader(reportGeneratorService.getProjectReportConfigs());
        header.setTitle("Reports");
        this.add(header);
    }

}

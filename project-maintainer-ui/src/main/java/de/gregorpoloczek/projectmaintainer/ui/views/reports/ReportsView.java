package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportGeneratorService;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;

@Route(value = "/reports", layout = MainLayout.class)
public class ReportsView extends VerticalLayout {

    public ReportsView(
            ProjectReportGeneratorService projectReportGeneratorService) {
        ReportHeader header = new ReportHeader(projectReportGeneratorService.getProjectReportConfigs());
        header.setTitle("Reports");
        this.add(header);
    }

}

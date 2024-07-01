package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("/reports")
public class ReportsView extends VerticalLayout {

    public ReportsView(
            ReportingProperties reportingProperties) {
        ReportHeader header = new ReportHeader(reportingProperties);
        header.setTitle("Reports");
        this.add(header);
    }

}

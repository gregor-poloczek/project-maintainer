package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import java.util.List;

@Route("/reports")
public class ReportsView extends VerticalLayout {

    private final ReportHeader header;
    private final ReportingProperties reportingProperties;

    public ReportsView(
            ReportingProperties reportingProperties) {
        this.reportingProperties = reportingProperties;
        this.header = new ReportHeader(reportingProperties);
        this.header.setTitle("Reports");

        this.add(this.header);
    }


}

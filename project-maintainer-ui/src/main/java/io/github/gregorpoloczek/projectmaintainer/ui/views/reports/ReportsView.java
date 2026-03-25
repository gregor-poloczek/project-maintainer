package io.github.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import io.github.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService;
import io.github.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import lombok.experimental.UtilityClass;

@Route(value = "/workspace/:workspaceId/reports", layout = MainLayout.class)
public class ReportsView extends VerticalLayout implements BeforeEnterObserver {
    @UtilityClass
    public class Parameters {
        public static final String WORKSPACE_ID = "workspaceId";
    }

    private final ReportGeneratorService reportGeneratorService;

    public ReportsView(ReportGeneratorService reportGeneratorService) {
        this.reportGeneratorService = reportGeneratorService;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        String workspaceId = beforeEnterEvent.getRouteParameters().get(Parameters.WORKSPACE_ID).orElseThrow();
        ReportHeader header = new ReportHeader(workspaceId, reportGeneratorService.getProjectReportConfigs());
        header.setTitle("Reports");
        this.add(header);
    }
}

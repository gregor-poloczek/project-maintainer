package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties;
import de.gregorpoloczek.projectmaintainer.reporting.ReportingProperties.ReportProperties;

public class ReportHeader extends FlexLayout {

    private final ComboBox<ReportProperties> comboBox;
    private H1 title;

    void setTitle(String title) {
        this.title.setText(title);
    }

    public ReportHeader(ReportingProperties reportingProperties) {
        this.title = new H1("");
        comboBox = new ComboBox<>("Report");
        comboBox.setItems(reportingProperties.getReports());
        comboBox.setItemLabelGenerator(ReportProperties::getName);
        comboBox.addValueChangeListener(e -> comboBox.getUI()
                .ifPresent(
                        ui -> {
                            if (e.getValue() == null) {
                                ui.navigate(ReportsView.class);
                            } else {
                                ui.navigate(ReportView.class, new RouteParameters("reportId", e.getValue().getId()));
                            }
                        }));
        this.setJustifyContentMode(JustifyContentMode.BETWEEN);
        this.setWidth("100%");
        add(title, comboBox);
    }


    public void setSelectedReport(ReportProperties report) {
        this.comboBox.setValue(report);
    }
}

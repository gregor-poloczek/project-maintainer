package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.config.ReportConfig;
import java.util.ArrayList;
import java.util.List;

public class ReportHeader extends FlexLayout {

    private final ComboBox<ReportConfig> comboBox;
    private H1 title;

    void setTitle(String title) {
        this.title.setText(title);
    }

    public ReportHeader(List<? extends ReportConfig> reportConfigs) {
        this.title = new H1("");
        comboBox = new ComboBox<>("Report");
        comboBox.setItems(new ArrayList<>(reportConfigs));
        comboBox.setItemLabelGenerator(ReportConfig::getName);
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


    public void setSelectedReport(ReportConfig report) {
        this.comboBox.setValue(report);
    }
}

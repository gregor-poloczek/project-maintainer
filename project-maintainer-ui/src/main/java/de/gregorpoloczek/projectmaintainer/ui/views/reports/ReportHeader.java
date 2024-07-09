package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportConfig;
import java.util.ArrayList;
import java.util.List;

public class ReportHeader extends FlexLayout {

    private final ComboBox<ReportConfig> comboBox;
    private final ProgressBar progressBar;
    private final H1 title;

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
        this.setAlignItems(Alignment.END);
        this.setWidth("100%");

        Div spacer1 = new Div();
        spacer1.setWidth("20px");
        Div spacer2 = new Div();
        spacer2.setWidth("20px");

        this.progressBar = new ProgressBar();
        this.progressBar.setVisible(false);

        add(title, spacer1, progressBar, spacer2, comboBox);
    }


    public void setSelectedReport(ReportConfig report) {
        this.progressBar.setVisible(report != null);
        this.comboBox.setValue(report);
    }

    public void updateProgress(int progressCurrent, int progressTotal) {
        this.progressBar.setMax(progressTotal);
        this.progressBar.setValue(progressCurrent);
    }
}

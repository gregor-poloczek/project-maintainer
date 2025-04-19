package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style.WhiteSpace;
import com.vaadin.flow.router.RouteParameters;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportConfig;
import java.util.ArrayList;
import java.util.List;

public class ReportHeader extends FlexLayout {

    private final ComboBox<ReportConfig> comboBox;
    private final H2 title;

    void setTitle(String title) {
        this.title.setText(title);
    }

    public ReportHeader(List<? extends ReportConfig> reportConfigs) {
        this.title = new H2("");
        this.title.getStyle().setWhiteSpace(WhiteSpace.NOWRAP);
        comboBox = new ComboBox<>();
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
        this.setAlignItems(Alignment.CENTER);
        this.setWidth("100%");

        Div spacer1 = new Div();
        spacer1.setWidth("20px");
        Div spacer2 = new Div();
        spacer2.setWidth("20px");

        HorizontalLayout horizontalLayout = new HorizontalLayout(new Text("Report: "), comboBox);
        horizontalLayout.setAlignItems(Alignment.CENTER);
        add(title, spacer2, horizontalLayout);
    }


    public void setSelectedReport(ReportConfig report) {
        this.comboBox.setValue(report);
    }

}

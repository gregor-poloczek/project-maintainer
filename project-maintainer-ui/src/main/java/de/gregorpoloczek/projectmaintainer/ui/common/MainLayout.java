package de.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNavItem;
import de.gregorpoloczek.projectmaintainer.ui.views.analysis.AnalysisView;
import de.gregorpoloczek.projectmaintainer.ui.views.git.GitView;
import de.gregorpoloczek.projectmaintainer.ui.views.patching.PatchesView;
import de.gregorpoloczek.projectmaintainer.ui.views.reports.ReportsView;

public class MainLayout extends AppLayout {

    public MainLayout() {
        DrawerToggle toggle = new DrawerToggle();

        Span viewTitle = new Span("Project Maintainer");
        addToNavbar(toggle, viewTitle);

        VerticalLayout menuLayout = new VerticalLayout();
        menuLayout.add(new SideNavItem("Projects", GitView.class, VaadinIcon.FOLDER_ADD.create()));
        menuLayout.add(new SideNavItem("Analysis", AnalysisView.class, VaadinIcon.SEARCH.create()));
        menuLayout.add(new SideNavItem("Reports", ReportsView.class, VaadinIcon.TABLE.create()));
        menuLayout.add(new SideNavItem("Patches", PatchesView.class, VaadinIcon.FILE_PROCESS.create()));

        addToDrawer(menuLayout);
    }
}

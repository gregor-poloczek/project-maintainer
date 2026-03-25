package io.github.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.ui.views.analysis.AnalysisView;
import io.github.gregorpoloczek.projectmaintainer.ui.views.projects.ProjectView;
import io.github.gregorpoloczek.projectmaintainer.ui.views.patching.PatchesView;
import io.github.gregorpoloczek.projectmaintainer.ui.views.reports.ReportsView;
import io.github.gregorpoloczek.projectmaintainer.ui.views.workspace.WorkspaceView;
import io.github.gregorpoloczek.projectmaintainer.ui.views.workspaces.WorkspacesView;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;

import java.util.*;

@Uses(TextField.class)
@Uses(PasswordField.class)
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    private final VerticalLayout itemsLayout;
    private final List<SideNavItem> navItems;
    private String selectedWorkspaceId;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (beforeEnterEvent.getLocation().getPath().equals("")) {
            beforeEnterEvent.rerouteTo(WorkspacesView.class);
            return;
        }
        List<String> segments = beforeEnterEvent.getLocation().getSegments();
        if (segments.size() > 1 && segments.getFirst().equals("workspace")) {
            String workspace = segments.get(1);

            this.selectedWorkspaceId = workspace;

            navItems.forEach(ni -> {
                ni.setPath(ni.getPath().replaceAll("^workspace/([^/]+)(/(.+))?$", "workspace/%s/$3".formatted(selectedWorkspaceId)));
            });
        } else {
            this.selectedWorkspaceId = null;
        }
        this.navItems.forEach(ni ->
                ni.setEnabled(!ni.getPath().matches("^workspace/.*$") || this.selectedWorkspaceId != null));
    }


    public MainLayout(WorkspaceService workspaceService) {
        DrawerToggle toggle = new DrawerToggle();

        Span viewTitle = new Span("Project Maintainer");
        addToNavbar(toggle, viewTitle);
        itemsLayout = new VerticalLayout();
        itemsLayout.setPadding(false);

        RouteParameters defaultRouteParameters = new RouteParameters(new RouteParam("workspaceId", "-1"));
        this.navItems = List.of(
                new SideNavItem("Workspaces", WorkspacesView.class, MaterialIcons.WORKSPACES.create()),
                new SideNavItem("Workspace", WorkspaceView.class, defaultRouteParameters, MaterialIcons.WORKSPACES.create()),
                new SideNavItem("Projects", ProjectView.class, defaultRouteParameters, MaterialIcons.FOLDER.create()),
                new SideNavItem("Analysis", AnalysisView.class, defaultRouteParameters, MaterialIcons.SEARCH.create()),
                new SideNavItem("Reports", ReportsView.class, defaultRouteParameters, MaterialIcons.TABLE_CHART.create()),
                new SideNavItem("Patches", PatchesView.class, defaultRouteParameters, MaterialIcons.AUTO_FIX_HIGH.create())
        );
        for (SideNavItem navItem : navItems) {
            navItem.setEnabled(false);
            itemsLayout.add(navItem);
        }

        addToDrawer(new VerticalLayout(itemsLayout));
    }
}

package de.gregorpoloczek.projectmaintainer.core.common.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.common.ui.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.Base64;
import java.util.Optional;
import reactor.core.Disposable;

@Route
public class MainView extends VerticalLayout {

    public final LitRenderer<Project> iconRenderer =
            LitRenderer.<Project>of("<img src=${item.image} style=\"height:32px;\" />")
                    .withProperty("image", project -> {
                        Optional<Image> image = MainView.this.imageResolverService.getImage("gitprovider",
                                project.getMetaData().getGitProvider()
                                        .name());

                        return image.map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                                .encodeToString(i.getBytes())).orElse("");
                    });


    private final ProjectService projectService;
    private final OperationExecutionService operationExecutionService;
    private final ImageResolverService imageResolverService;
    private final Grid<Project> grid;
    private Disposable subscription;

    public MainView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            OperationExecutionService operationExecutionService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.operationExecutionService = operationExecutionService;

        grid = new Grid<>(Project.class, false);
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.addColumn(this.iconRenderer).setFlexGrow(0);
        grid.addColumn(p -> p.getMetaData().getName()).setHeader("Name");

        MenuBar menuBar = createManuBar();

        this.add(menuBar);
        this.add(grid);
    }

    private MenuBar createManuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Clone / Pull", (e) -> {
            for (Project project : grid.getSelectionModel().getSelectedItems()) {
                if (project.isCloned()) {
                    operationExecutionService.executeAsyncOperation(
                            project,
                            "git::pull",
                            this.projectService::pullProject);
                }
            }
        });

        menuBar.addItem("Wipe", (e) -> {
            for (Project project : grid.getSelectionModel().getSelectedItems()) {
                System.out.println(project.getFQPN());
            }
        });
        return menuBar;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        subscription = this.operationExecutionService.getUpdateEvents().subscribe((e) -> {
            System.out.println(e);
        });
        grid.setItems(projectService.getProjects());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.subscription.dispose();
    }
}

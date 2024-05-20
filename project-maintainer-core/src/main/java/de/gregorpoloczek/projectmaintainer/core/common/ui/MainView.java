package de.gregorpoloczek.projectmaintainer.core.common.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.common.ui.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;
import reactor.core.Disposable;

@Route
public class MainView extends VerticalLayout {

    @Getter
    @Setter
    @Builder
    public static class ProjectItem {

        private final Project project;
        private Optional<Image> image;
        private String text = "";
    }

    public final LitRenderer<ProjectItem> iconRenderer =
            LitRenderer.<ProjectItem>of("<img src=${item.image} style=\"height:32px;\" />")
                    .withProperty("image", item -> {
                        Optional<Image> image = item.getImage();
                        return image.map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                                .encodeToString(i.getBytes())).orElse("");
                    });


    private final ProjectService projectService;
    private final OperationExecutionService operationExecutionService;
    private final ImageResolverService imageResolverService;
    private final Grid<ProjectItem> grid;
    private Map<FQPN, ProjectItem> itemByFQPN;
    private Disposable subscription;

    public MainView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            OperationExecutionService operationExecutionService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.operationExecutionService = operationExecutionService;

        grid = new Grid<>(ProjectItem.class, false);
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.addColumn(this.iconRenderer).setFlexGrow(0);
        grid.addColumn(p -> p.project.getMetaData().getName()).setHeader("Name");
        grid.addColumn(ProjectItem::getText).setHeader("Info");

        MenuBar menuBar = createManuBar();

        this.add(menuBar);
        this.add(grid);
    }

    private MenuBar createManuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Clone / Pull", (e) -> {
            for (ProjectItem item : grid.getSelectionModel().getSelectedItems()) {
                if (item.project.isCloned()) {
                    operationExecutionService.executeAsyncOperation(
                            item.project,
                            "git::pull",
                            this.projectService::pullProject);
                } else {
                    throw new NotImplementedException("asdasd");
                }
            }
        });

        menuBar.addItem("Wipe", (e) -> {
            throw new NotImplementedException("asdasd");
        });
        return menuBar;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<ProjectItem> items = projectService.getProjects().stream()
                .map(p -> ProjectItem.builder()
                        .project(p)
                        .image(MainView.this.imageResolverService.getImage("gitprovider",
                                p.getMetaData().getGitProvider().name())).text("").build())
                .collect(Collectors.toList());

        itemByFQPN = items.stream().collect(Collectors.toMap(p -> p.getProject().getFQPN(), Function.identity()));

        grid.setItems(items);

        UI current = UI.getCurrent();
        subscription = this.operationExecutionService.getUpdateEvents().subscribe((e) -> {
            ProjectItem item = itemByFQPN.get(e.getFqpn());
            current.access(() -> {
                String text = switch (e.getState()) {
                    case SCHEDULED -> e.getOperation() + " ...";
                    case RUNNING -> Double.toString(e.getProgress());
                    case SUCCEEDED -> "";
                    default -> e.getState().name();
                };
                item.setText(text);
                this.grid.getDataProvider().refreshItem(item);
            });
        });

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.subscription.dispose();
        this.subscription = null;
    }
}

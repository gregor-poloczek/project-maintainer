package de.gregorpoloczek.projectmaintainer.core.common.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.common.ui.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import reactor.core.Disposable;

@Route
public class MainView extends VerticalLayout {

    @Getter
    @Setter
    @Builder
    public static class ProjectItem {

        private ProjectOperationState operationState = null;
        private Project project;
        private Optional<Image> image;
        private String text = "";
        private boolean operationInProgress;
        private Double operationProgressValue;
    }

    public final LitRenderer<ProjectItem> iconRenderer =
            LitRenderer.<ProjectItem>of(
                            "<img src=${item.image} style=\"height:32px; filter: grayscale(${item.grayscale});\" />")
                    .withProperty("grayscale", item -> item.project.isCloned() ? "0.0" : "1.0")
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
        grid.addColumn(createProgressBarRenderer());

        MenuBar menuBar = createManuBar();

        this.add(menuBar);
        this.add(grid);
    }

    private static ComponentRenderer<VerticalLayout, ProjectItem> createProgressBarRenderer() {
        return new ComponentRenderer<>(item -> {
            Div progressBarLabelText = new Div();
            progressBarLabelText.setText(item.getText());

            Div progressBarLabelValue = new Div();
            progressBarLabelValue.setText(
                    item.getOperationProgressValue() != null ? MessageFormat.format("{0,number,#.#}%",
                            item.getOperationProgressValue() * 100) : "");
            FlexLayout progressBarLabel = new FlexLayout();
            progressBarLabel.setJustifyContentMode(JustifyContentMode.BETWEEN);
            progressBarLabel.add(progressBarLabelText, progressBarLabelValue);

            ProgressBar progressBar = new ProgressBar();
            if (item.getOperationProgressValue() != null) {
                progressBar.setValue(item.getOperationProgressValue());
                progressBar.setIndeterminate(false);
            } else if (item.getOperationState() != null && item.getOperationState().isTerminated()) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(1);
            } else {
                progressBar.setIndeterminate(true);
            }
            progressBar.setVisible(item.operationState != null);
            progressBar.removeThemeVariants(ProgressBarVariant.LUMO_SUCCESS, ProgressBarVariant.LUMO_ERROR,
                    ProgressBarVariant.LUMO_CONTRAST);
            if (item.operationState == ProjectOperationState.SUCCEEDED) {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            } else if (item.operationState == ProjectOperationState.FAILED) {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_ERROR);
            } else {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_CONTRAST);
            }

            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.add(progressBarLabel, progressBar);
            layout.setPadding(false);

            return layout;
        });
    }

    private MenuBar createManuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Clone / Pull", this::onClonePullClick);
        menuBar.addItem("Wipe", this::onWipeClick);
        return menuBar;
    }

    private void onWipeClick(ClickEvent<MenuItem> event) {
        for (ProjectItem item : grid.getSelectionModel().getSelectedItems()) {
            if (!item.project.isCloned()) {
                continue;
            }
            operationExecutionService.executeAsyncOperation(
                    item.project,
                    "git::wipe",
                    this.projectService::wipeProject);
        }
    }

    private void onClonePullClick(ClickEvent<MenuItem> event) {
        for (ProjectItem item : grid.getSelectionModel().getSelectedItems()) {
            if (item.project.isCloned()) {
                operationExecutionService.executeAsyncOperation(
                        item.project,
                        "git::pull",
                        this.projectService::pullProject);
            } else {
                operationExecutionService.executeAsyncOperation(
                        item.project,
                        "git::clone",
                        this.projectService::cloneProject);
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<ProjectItem> items = projectService.getProjects().stream()
                .map(this::toProjectItem)
                .collect(Collectors.toList());

        itemByFQPN = items.stream().collect(Collectors.toMap(p -> p.getProject().getFQPN(), Function.identity()));

        grid.setItems(items);

        UI current = UI.getCurrent();
        subscription = this.operationExecutionService.getUpdateEvents().subscribe((e) -> {
            ProjectItem item = itemByFQPN.get(e.getFqpn());
            current.access(() -> {
                String text = switch (e.getState()) {
                    case SCHEDULED -> e.getOperation() + " ...";
                    case RUNNING -> e.getMessage();
                    case SUCCEEDED -> "";
                    default -> e.getState().name();
                };

                item.setOperationInProgress(!e.getState().isTerminated());
                item.setOperationProgressValue(e.getProgress() == -1 ? null : e.getProgress());
                item.setOperationState(e.getState());
                if (e.getState() == ProjectOperationState.SUCCEEDED) {
                    ProjectItem newItem = toProjectItem(this.projectService.getProject(e.getFqpn())
                            .orElseThrow(() -> new ProjectNotFoundException(e.getFqpn())));
                    item.setText(newItem.getText());
                    item.setProject(newItem.getProject());
                }
                item.setText(text);

                this.grid.getDataProvider().refreshItem(item);
            });
        });

    }

    private ProjectItem toProjectItem(Project p) {
        String text = p.isCloned() ? "" : "Not cloned";
        return ProjectItem.builder()
                .project(p)
                .text(text)
                .image(MainView.this.imageResolverService.getImage("gitprovider",
                        p.getMetaData().getGitProvider().name())).build();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.subscription.dispose();
        this.subscription = null;
    }
}

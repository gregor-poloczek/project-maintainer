package de.gregorpoloczek.projectmaintainer.core.common.ui.git;

import static java.util.stream.Collectors.toList;

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
import de.gregorpoloczek.projectmaintainer.core.common.ui.shared.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.core.common.ui.shared.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
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
import reactor.core.Disposable;

@Route
public class GitView extends VerticalLayout {

    private final LitRenderer<ProjectItem> iconRenderer =
            LitRenderer.<ProjectItem>of(
                            "<img src=${item.image} style=\"height:32px; filter: grayscale(${item.grayscale});\" />")
                    .withProperty("grayscale", item -> item.getProject().isCloned() ? "0.0" : "1.0")
                    .withProperty("image", item -> {
                        Optional<Image> image = item.getImage();
                        return image.map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                                .encodeToString(i.getBytes())).orElse("");
                    });


    private final transient ProjectService projectService;
    private final transient OperationExecutionService operationExecutionService;
    private final transient ImageResolverService imageResolverService;
    private final Grid<ProjectItem> grid;
    private transient Map<FQPN, ProjectItem> itemByFQPN;
    private transient Disposable subscription;

    public GitView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            OperationExecutionService operationExecutionService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.operationExecutionService = operationExecutionService;

        this.grid = createGrid();

        MenuBar menuBar = createManuBar();

        this.add(menuBar);
        this.add(grid);
    }

    private Grid<ProjectItem> createGrid() {
        final Grid<ProjectItem> result;
        result = new Grid<>(ProjectItem.class, false);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(this.iconRenderer).setFlexGrow(0);
        result.addColumn(ProjectItem::getName).setHeader("Name");
        result.addColumn(createProgressBarRenderer());
        return result;
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
            progressBar.setVisible(item.getOperationState() != null);
            progressBar.removeThemeVariants(ProgressBarVariant.LUMO_SUCCESS, ProgressBarVariant.LUMO_ERROR,
                    ProgressBarVariant.LUMO_CONTRAST);
            if (item.getOperationState() == ProjectOperationState.SUCCEEDED) {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            } else if (item.getOperationState() == ProjectOperationState.FAILED) {
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
            if (!item.getProject().isCloned()) {
                continue;
            }
            operationExecutionService.executeAsyncOperation(
                    item.getProject(),
                    "git::wipe",
                    this.projectService::wipeProject);
        }
    }

    private void onClonePullClick(ClickEvent<MenuItem> event) {
        for (ProjectItem item : grid.getSelectionModel().getSelectedItems()) {
            if (item.getProject().isCloned()) {
                operationExecutionService.executeAsyncOperation(
                        item.getProject(),
                        "git::pull",
                        this.projectService::pullProject);
            } else {
                operationExecutionService.executeAsyncOperation(
                        item.getProject(),
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
                .toList();

        this.itemByFQPN = items.stream().collect(Collectors.toMap(p -> p.getProject().getFQPN(), Function.identity()));

        this.grid.setItems(items);

        final UI current = UI.getCurrent();
        subscription = this.operationExecutionService.getUpdateEvents().subscribe(e -> onUpdateEvent(e, current));

    }

    private void onUpdateEvent(ProjectOperationProgress e, UI current) {
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
    }

    private ProjectItem toProjectItem(Project p) {
        String text = p.isCloned() ? "" : "Not cloned";
        return ProjectItem.builder()
                .project(p)
                .text(text)
                .owner(p.getMetaData().getOwner())
                .image(GitView.this.imageResolverService.getImage("gitprovider",
                        p.getMetaData().getGitProvider().name())).build();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.subscription.dispose();
        this.subscription = null;
    }
}

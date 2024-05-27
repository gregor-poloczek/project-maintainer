package de.gregorpoloczek.projectmaintainer.core.common.ui.git;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.common.ui.shared.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.core.common.ui.shared.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.time4j.PrettyTime;

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
    private final WorkingCopyService workingCopyService;
    private transient Map<FQPN, ProjectItem> itemByFQPN;

    public GitView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            OperationExecutionService operationExecutionService,
            WorkingCopyService workingCopyService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.operationExecutionService = operationExecutionService;
        this.workingCopyService = workingCopyService;

        this.grid = createGrid();

        MenuBar menuBar = createManuBar();

        this.add(menuBar);
        this.add(grid);
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private Grid<ProjectItem> createGrid() {
        final Grid<ProjectItem> result;
        result = new Grid<>(ProjectItem.class, false);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(this.iconRenderer).setFlexGrow(0).setWidth("64px");
        result.addColumn(createNameRenderer()).setHeader("Name");
        result.addColumn(createWorkingCopyRendered()).setHeader("Working copy");
        result.addColumn(this.createProgressBarRenderer());
        return result;
    }

    private ComponentRenderer<VerticalLayout, ProjectItem> createProgressBarRenderer() {
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

    private ComponentRenderer<FlexLayout, ProjectItem> createWorkingCopyRendered() {
        return new ComponentRenderer<>(item -> {
            FlexLayout layout = new FlexLayout();
            layout.setFlexDirection(FlexDirection.COLUMN);
            Text message = new Text("");

            Span timestamp = createBadge();
            Span hash = createBadge();
            Span authorName = createBadge();

            HorizontalLayout badges = new HorizontalLayout();
            badges.add(hash, authorName, timestamp);

            layout.add(badges, message);

            Optional<Commit> maybeCommit = item.getLatestCommit();
            maybeCommit.ifPresent(commit -> {
                timestamp.setText(PrettyTime.of(Locale.US)
                        .printRelative(commit.getTimestamp(), TimeZone.getDefault().toZoneId()));
                timestamp.setTitle(commit.getTimestamp().toString());
                hash.setText(commit.getHash());
                authorName.setText(commit.getAuthorName());
            });
            badges.setVisible(maybeCommit.isPresent());
            message.setText(maybeCommit.map(Commit::getMessage).orElse(""));
            return layout;
        });
    }

    private Span createBadge() {
        Span timestamp = new Span("");
        timestamp.getElement().getThemeList().add("badge");
        return timestamp;
    }

    private ComponentRenderer<FlexLayout, ProjectItem> createNameRenderer() {
        return new ComponentRenderer<>(item -> {
            FlexLayout layout = new FlexLayout();
            HorizontalLayout badges = new HorizontalLayout();
            layout.setFlexDirection(FlexDirection.COLUMN);

            Component name;
            if (item.getProject().getMetaData().getBrowserLink().isPresent()) {
                Anchor anchor = new Anchor();
                anchor.setHref(item.getProject().getMetaData().getBrowserLink().get());
                anchor.setTarget("_blank");
                name = anchor;
            } else {
                name = new Text("");
            }
            ((HasText) name).setText(item.getName());

            Span prefix = createBadge();
            prefix.setText(item.getNamePrefix());
            badges.add(prefix);
            layout.add(badges, name);
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
        UI ui = UI.getCurrent();
        for (ProjectItem item : grid.getSelectionModel().getSelectedItems()) {
            if (!item.getProject().isCloned()) {
                continue;
            }
            operationExecutionService.executeAsyncOperation2(
                    item.getProject(),
                    "git::wipe",
                    this.projectService::wipeProject).subscribe(e ->
                    onUpdateEvent(e, ui));
        }
    }

    private void onClonePullClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();
        for (ProjectItem item : grid.getSelectionModel().getSelectedItems()) {
            if (item.getProject().isCloned()) {
                operationExecutionService.executeAsyncOperation2(
                        item.getProject(),
                        "git::pull",
                        this.projectService::pullProject).subscribe(e ->
                        onUpdateEvent(e, ui));
            } else {
                operationExecutionService.executeAsyncOperation2(
                        item.getProject(),
                        "git::clone",
                        this.projectService::cloneProject).subscribe(e ->
                        onUpdateEvent(e, ui));
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<ProjectItem> items = projectService.getProjects().stream()
                .map(this::toProjectItem)
                .toList();

        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(p -> p.getProject().getMetaData().getFQPN(), Function.identity()));

        this.grid.setItems(items);
    }

    private void onUpdateEvent(ProjectOperationProgress e, UI current) {
        if (!current.isAttached()) {
            return;
        }
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
                .latestCommit(
                        this.workingCopyService.find(p.getMetaData().getFQPN()).flatMap(WorkingCopy::getLatestCommit))
                .owner(p.getMetaData().getOwner())
                .image(GitView.this.imageResolverService.getImage("gitprovider",
                        p.getMetaData().getGitProvider().name())).build();
    }

}

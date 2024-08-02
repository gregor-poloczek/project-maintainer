package de.gregorpoloczek.projectmaintainer.ui.views.git;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.MainLayout;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RouteAlias(value = "/", layout = MainLayout.class)
@Route(value = "/git", layout = MainLayout.class)
public class GitView extends VerticalLayout {

    private final transient ProjectService projectService;
    private final transient ImageResolverService imageResolverService;
    private final transient WorkingCopyService workingCopyService;
    private Map<FQPN, ProjectItem> itemByFQPN;
    private final MenuBar menuBar;
    private final TextField search;
    private final Grid<ProjectItem> grid;

    private final transient Disposable.Swap currentOperation = Disposables.swap();


    private final Renderer<ProjectItem> progressBarRenderer = new ComponentRenderer<>(item -> {
        Div progressBarLabelText = new Div();
        progressBarLabelText.setText(item.getText());

        Div progressBarLabelValue = new Div();
        progressBarLabelValue.setText(
                item.getOperationProgressValue() != null ? MessageFormat.format("{0,number,#.#}%",
                        item.getOperationProgressValue() * 100) : "");
        FlexLayout top = new FlexLayout();
        top.setWidth("100%");
        top.setJustifyContentMode(JustifyContentMode.BETWEEN);
        top.setFlexDirection(FlexDirection.ROW);
        top.add(progressBarLabelText, progressBarLabelValue);

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
        if (item.getOperationState() == OperationProgress.State.DONE) {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
        } else if (item.getOperationState() == OperationProgress.State.FAILED) {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_ERROR);
        } else {
            progressBar.addThemeVariants(ProgressBarVariant.LUMO_CONTRAST);
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.add(top, progressBar);
        layout.setPadding(false);

        return layout;
    });


    public GitView(
            ProjectService projectService,
            ImageResolverService imageResolverService,
            WorkingCopyService workingCopyService) {
        this.projectService = projectService;
        this.imageResolverService = imageResolverService;
        this.workingCopyService = workingCopyService;

        this.grid = createGrid();

        this.menuBar = createMenuBar();

        this.search = new TextField();
        this.search.setPlaceholder("Search");
        this.search.setValueChangeMode(ValueChangeMode.EAGER);
        this.search.addValueChangeListener(e -> {
            ListDataProvider<ProjectItem> dataProvider = (ListDataProvider<ProjectItem>) this.grid.getDataProvider();
            String query = e.getValue().toLowerCase();
            dataProvider.setFilter(
                    i -> StringUtils.isBlank(query) || i.matches(query));
            // TODO brauch ich das?
            dataProvider.refreshAll();
        });

        this.add(this.search, menuBar, grid);
        this.setSizeFull();
        this.grid.setSizeFull();
    }

    private Grid<ProjectItem> createGrid() {
        final Grid<ProjectItem> result;
        result = new Grid<>(ProjectItem.class, false);
        result.setSelectionMode(SelectionMode.MULTI);
        result.addColumn(Renderers.getIconRenderer()).setFlexGrow(0).setWidth("64px");
        result.addColumn(Renderers.getNameRenderer()).setHeader("Name");
        result.addColumn(new ComponentRenderer<>(i -> {
            HorizontalLayout r = new HorizontalLayout();
            if (StringUtils.isNotBlank(i.getWebsite())) {
                Anchor anchor = new Anchor();
                anchor.add(VaadinIcon.GLOBE_WIRE.create());
                anchor.setTarget("_blank");
                anchor.setHref(i.getWebsite());
                r.add(anchor);
            }
            return r;
        })).setFlexGrow(0).setWidth("64px");
        result.addColumn(
                LitRenderer.<ProjectItem>of(
                                "<div style=\"text-wrap: balance;\">${item.text}</div>")
                        .withProperty("text", ProjectItem::getDescription)
                        .withProperty("grayscale", item -> !item.isIconBlurred() ? "0.0" : "1.0")
                        .withProperty("image", item -> {
                            Optional<Image> image = item.getIcon();
                            return image.map(
                                    i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                                            .encodeToString(i.getBytes())).orElse("");
                        })).setHeader("Description");
        result.addColumn(Renderers.getWorkingCopyRenderer()).setHeader("Working copy");
        result.addColumn(this.progressBarRenderer);
        return result;
    }

    private MenuBar createMenuBar() {
        MenuBar result = new MenuBar();
        result.addItem("Attach", this::onAttachClick);
        result.addItem("Pull", this::onPullClick);
        result.addItem("Detach", this::onDetachClick);
        return result;
    }

    private void onDetachClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .filter(item -> item.getWorkingCopy().isPresent())
                .flatMap(item ->
                        this.workingCopyService.wipeProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnTerminate(() -> this.unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onUpdateEvent(p, ui));
        currentOperation.update(subscription);
    }

    private void onAttachClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();
        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .filter(item -> item.getWorkingCopy().isEmpty())
                .flatMap(item ->
                        this.workingCopyService.cloneProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnTerminate(() -> this.unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onUpdateEvent(p, ui));

        currentOperation.update(subscription);
    }

    private void onPullClick(ClickEvent<MenuItem> event) {
        UI ui = UI.getCurrent();

        Disposable subscription = Flux.fromIterable(grid.getSelectionModel().getSelectedItems())
                .filter(item -> item.getWorkingCopy().isPresent())
                .flatMap(item ->
                        this.workingCopyService.pullProject(item)
                                .subscribeOn(Schedulers.parallel()))
                .doOnSubscribe(s -> this.lockOperations(ui))
                .doOnTerminate(() -> unlockOperations(ui))
                // TODO cancelation handling
                .subscribe(p -> onUpdateEvent(p, ui));

        currentOperation.update(subscription);
    }


    private void unlockOperations(UI ui) {
        ui.access(() -> this.menuBar.setEnabled(true));
    }

    private void lockOperations(UI ui) {
        ui.access(() -> this.menuBar.setEnabled(false));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.currentOperation.dispose();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<ProjectItem> items = projectService.findALl().stream()
                .map(this::toProjectItem)
                .toList();

        this.itemByFQPN = items.stream()
                .collect(Collectors.toMap(p -> p.getProject().getMetaData().getFQPN(), Function.identity()));

        this.grid.setItems(items);
    }

    private void onUpdateEvent(ProjectOperationProgress<?> e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the mean time
            return;
        }
        ProjectItem item = itemByFQPN.get(e.getFQPN());
        current.access(() -> {
            String text = switch (e.getState()) {
                case OperationProgress.State.SCHEDULED ->
                        Optional.ofNullable(e.getMessage()).filter(StringUtils::isNotBlank).orElse("...");
                case OperationProgress.State.RUNNING -> e.getMessage();
                case OperationProgress.State.DONE -> "";
                case OperationProgress.State.FAILED -> "Operation failed";
                default -> e.getState().name();
            };
            // TODO error handling

            double progress = (double) e.getProgressCurrent() / (double) e.getProgressTotal();
            if (Double.isNaN(progress) || Double.isInfinite(progress)) {
                progress = 0.0d;
            }
            item.setOperationInProgress(!e.getState().isTerminated());
            item.setOperationProgressValue(progress);
            item.setOperationState(e.getState());
            if (e.getState() == OperationProgress.State.DONE) {
                ProjectItem newItem = toProjectItem(this.projectService.require(e));
                item.setText(newItem.getText());
                item.setProject(newItem.getProject());
                item.setWorkingCopy(this.workingCopyService.find(e).orElse(null));
                item.setIcon(this.imageResolverService.getProjectImage(newItem.getProject()).orElse(null));
            }
            item.setText(text);

            this.grid.getDataProvider().refreshItem(item);
        });
    }

    private ProjectItem toProjectItem(Project p) {
        ProjectMetaData metaData = p.getMetaData();
        Optional<WorkingCopy> workingCopy = this.workingCopyService.find(metaData.getFQPN());
        String text = workingCopy.isPresent() ? "" : "Not attached";
        return ProjectItem.builder()
                .project(p)
                .text(text)
                .description(metaData.getDescription().orElse(""))
                .website(metaData.getWebsiteLink().orElse(""))
                .workingCopy(workingCopy.orElse(null))
                .owner(metaData.getOwner())
                .icon(this.imageResolverService.getProjectImage(p).orElse(null)).build();
    }

}

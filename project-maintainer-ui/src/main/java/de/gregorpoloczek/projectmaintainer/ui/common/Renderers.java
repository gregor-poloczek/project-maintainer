package de.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.scm.service.git.Commit;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasIcon;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasOperationProgress;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.components.HasProject;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import net.time4j.PrettyTime;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class Renderers {

    public static <I extends Composable<I>> Renderer<I> getProgressBarRenderer() {
        return new ComponentRenderer<>(item -> {
            Optional<OperationProgress<?>> maybeProgress = item.requireComponent(HasOperationProgress.class)
                    .getOperationProgress();

            VerticalLayout layout = new VerticalLayout();
            if (maybeProgress.isEmpty()) {
                return layout;
            }

            OperationProgress<?> progress = maybeProgress.get();
            Div progressBarLabelText = new Div();
            progressBarLabelText.setText(progress.getMessage());

            Div progressBarLabelValue = new Div();
            progressBarLabelValue.setText(
                    MessageFormat.format("{0,number,#.#}%",
                            progress.getProgressRelative() * 100));
            FlexLayout top = new FlexLayout();
            top.setWidth("100%");
            top.setJustifyContentMode(JustifyContentMode.BETWEEN);
            top.setFlexDirection(FlexDirection.ROW);
            top.add(progressBarLabelText, progressBarLabelValue);

            ProgressBar progressBar = new ProgressBar();
            progressBar.setValue(progress.getProgressRelative());
            progressBar.setIndeterminate(false);

            progressBar.removeThemeVariants(ProgressBarVariant.LUMO_SUCCESS, ProgressBarVariant.LUMO_ERROR,
                    ProgressBarVariant.LUMO_CONTRAST);
            if (progress.getState() == OperationProgress.State.DONE) {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            } else if (progress.getState() == OperationProgress.State.FAILED) {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_ERROR);
            } else {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_CONTRAST);
            }

            layout.setSpacing(false);
            layout.add(top, progressBar);
            layout.setPadding(false);

            return layout;
        });
    }

    public interface HasWorkingCopy {

        Optional<WorkingCopy> getWorkingCopy();
    }

    public interface HasLabelsItem {

        Collection<Label> getLabels();
    }

    private static Span createBadge() {
        Span badge = new Span("");
        badge.getElement().getThemeList().add("badge");
        return badge;
    }

    public <I extends Composable<I>> Renderer<I> getIconRenderer() {
        return LitRenderer.<I>of(
                        "<img src=${item.image} style=\"height:48px; filter: grayscale(${item.grayscale});\" />")
                .withProperty("grayscale",
                        item -> !item.requireComponent(HasIcon.class).isBlurred() ? "0.0" : "1.0")
                .withProperty("image", item -> {
                    Optional<Image> image = item.requireComponent(HasIcon.class).getIcon();
                    return image.map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                            .encodeToString(i.getBytes())).orElse("");
                });

    }

    public <I extends HasWorkingCopy> Renderer<I> getWorkingCopyRenderer() {
        return new ComponentRenderer<>(item -> {
            FlexLayout layout = new FlexLayout();
            layout.setFlexDirection(FlexDirection.COLUMN);
            Div message = new Div("");
            message.getStyle().set("text-wrap", "balance");

            Span branch = createBadge();
            Span timestamp = createBadge();
            Span hash = createBadge();
            Span authorName = createBadge();

            HorizontalLayout badges = new HorizontalLayout();
            badges.add(branch, hash, authorName, timestamp);

            layout.add(badges, message);

            Optional<Commit> maybeCommit = item.getWorkingCopy().flatMap(WorkingCopy::getLatestCommit);
            branch.setText(item.getWorkingCopy().map(WorkingCopy::getCurrentBranch).orElse(""));

            maybeCommit.ifPresent(commit -> {
                timestamp.setText(PrettyTime.of(Locale.US)
                        .printRelative(commit.getTimestamp(), TimeZone.getDefault().toZoneId()));
                timestamp.setTitle(commit.getTimestamp().toString());
                hash.setText(commit.getHash());
                authorName.setText(commit.getAuthorName());
            });
            badges.setVisible(maybeCommit.isPresent());
            message.setText(maybeCommit
                    .map(Commit::getMessage)
                    .map(s -> StringUtils.abbreviate(s, 200))
                    .orElse(""));
            return layout;
        });
    }

    ;


    public <I extends HasLabelsItem> Renderer<I> getLabelsRenderer(Supplier<String> queryProvider) {
        return new ComponentRenderer<>((I item) -> {
            FlexLayout layout = new FlexLayout();
            layout.setFlexDirection(FlexDirection.ROW);
            layout.setFlexWrap(FlexWrap.WRAP);

            String query = queryProvider.get();

            List<Component> list = item.getLabels().stream()
                    .filter(l -> StringUtils.isBlank(query) || l.getValue().toLowerCase().contains(query))
                    .map(l -> {
                        HorizontalLayout wrapper = new HorizontalLayout();
                        wrapper.getStyle().set("padding", "4px");
                        Span badge = createBadge();

                        Component result;
                        if (StringUtils.isBlank(query)) {
                            result = new Span(l.getValue());
                        } else {
                            String adjusted =
                                    l.getValue()
                                            .replaceAll("(\\Q" + query + "\\E)", "<b style=\"color: white;\">$1</b>");
                            result = new Html("<span style=\"color: gray;\">" + adjusted + "</span>");
                        }
                        badge.add(result);
                        wrapper.add(badge);
                        return (Component) wrapper;
                    }).toList();

            layout.add(list);
            return layout;
        });
    }

    public <I extends Composable<I>> Renderer<I> getNameRenderer() {
        return new ComponentRenderer<>((I item) -> {
            FlexLayout layout = new FlexLayout();
            HorizontalLayout badges = new HorizontalLayout();
            layout.setFlexDirection(FlexDirection.COLUMN);

            Component name;
            Project project = item.requireComponent(HasProject.class).getProject();
            if (project.getMetaData().getBrowserLink().isPresent()) {
                Anchor anchor = new Anchor();
                anchor.setHref(project.getMetaData().getBrowserLink().get());
                anchor.setTarget("_blank");
                name = anchor;
            } else {
                name = new Text("");
            }
            ((HasText) name).setText(project.getMetaData().getName());

            Span prefix = createBadge();
            prefix.setText(getNamePrefix(project));
            badges.add(prefix);
            Div spacer = new Div();
            spacer.getStyle().set("height", "4px");
            layout.add(badges, spacer, name);
            return layout;
        });
    }

    public <C extends Composable<C>> Renderer<C> getProjectWebsiteLinkRenderer() {
        return new ComponentRenderer<>((C composable) -> {
            Optional<String> maybeWebsiteLink = composable.requireComponent(HasProject.class)
                    .getProject()
                    .getMetaData()
                    .getWebsiteLink()
                    .filter(StringUtils::isNotBlank);
            HorizontalLayout r = new HorizontalLayout();
            maybeWebsiteLink.ifPresent(websiteLink -> {
                Anchor anchor = new Anchor();
                anchor.add(VaadinIcon.GLOBE_WIRE.create());
                anchor.setTarget("_blank");
                anchor.setHref(websiteLink);
                r.add(anchor);
            });
            return r;
        });
    }

    private static String getNamePrefix(Project project) {
        return project.getMetaData().getFQPN().getSegments()
                .stream()
                .skip(1)
                .filter(s -> !s.equals(project.getMetaData().getName()))
                .collect(Collectors.joining(" / "));
    }

}

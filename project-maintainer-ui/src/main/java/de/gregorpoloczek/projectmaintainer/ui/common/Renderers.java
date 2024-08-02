package de.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.git.service.git.Commit;
import de.gregorpoloczek.projectmaintainer.git.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
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

    public interface HasProjectItem {

        Project getProject();
    }

    public interface HasWorkingCopy {

        Optional<WorkingCopy> getWorkingCopy();
    }

    public interface HasLabelsItem {

        Collection<Label> getLabels();
    }

    public interface HasIconItem {

        boolean isIconBlurred();

        Optional<Image> getIcon();
    }

    private static Span createBadge() {
        Span badge = new Span("");
        badge.getElement().getThemeList().add("badge");
        return badge;
    }

    public <I extends HasIconItem> Renderer<I> getIconRenderer() {
        return LitRenderer.<I>of(
                        "<img src=${item.image} style=\"height:48px; filter: grayscale(${item.grayscale});\" />")
                .withProperty("grayscale", item -> !item.isIconBlurred() ? "0.0" : "1.0")
                .withProperty("image", item -> {
                    Optional<Image> image = item.getIcon();
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

    public <I extends HasProjectItem> Renderer<I> getNameRenderer() {
        return new ComponentRenderer<>((I item) -> {
            FlexLayout layout = new FlexLayout();
            HorizontalLayout badges = new HorizontalLayout();
            layout.setFlexDirection(FlexDirection.COLUMN);

            Component name;
            Project project = item.getProject();
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

    private static String getNamePrefix(Project project) {
        return project.getMetaData().getFQPN().getSegments()
                .stream()
                .skip(1)
                .filter(s -> !s.equals(project.getMetaData().getName()))
                .collect(Collectors.joining(" / "));
    }

}

package de.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.scm.service.git.Commit;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasWorkingCopy;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import net.time4j.PrettyTime;
import org.apache.commons.lang3.StringUtils;

public class WorkingCopyStateComponent extends FlexLayout {

    private final Span branch;
    private final Span timestamp;
    private final Span hash;
    private final Span authorName;
    private final HorizontalLayout badges;
    private final Div message;

    public static <C extends Composable<C>> Renderer<C> getRenderer() {
        return new ComponentRenderer<>(WorkingCopyStateComponent::new,
                (component, composable) -> ((WorkingCopyStateComponent) component).update(composable));
    }


    public WorkingCopyStateComponent(Composable<?> composable) {
        this.message = new Div("");
        this.message.getStyle().set("text-wrap", "balance");

        this.branch = createBadge();
        this.timestamp = createBadge();
        this.hash = createBadge();
        this.authorName = createBadge();

        this.badges = new HorizontalLayout();
        this.badges.add(branch, hash, authorName, timestamp);

        this.add(badges, message);
        this.setFlexDirection(FlexDirection.COLUMN);
        this.update(composable);
    }

    public WorkingCopyStateComponent update(Composable<?> composable) {
        Optional<WorkingCopy> maybeWorkingCopy = composable.requireTrait(HasWorkingCopy.class).getWorkingCopy();
        Optional<Commit> maybeCommit = maybeWorkingCopy.flatMap(WorkingCopy::getLatestCommit);
        branch.setText(maybeWorkingCopy.map(WorkingCopy::getCurrentBranch).orElse(""));

        maybeCommit.ifPresent(commit -> {
            timestamp.setText(PrettyTime.of(Locale.US)
                    .printRelative(commit.getTimestamp(), TimeZone.getDefault().toZoneId()));
            timestamp.setTitle(commit.getTimestamp().toString());
            hash.setText(commit.getHash());
            authorName.setText(commit.getAuthorName());
        });
        badges.setVisible(maybeCommit.isPresent());
        message.setText(maybeCommit
                .map(Commit::getMessage).map(s -> StringUtils.abbreviate(s, 200))
                .orElse(""));
        return this;
    }

    private Span createBadge() {
        Span badge = new Span("");
        badge.getElement().getThemeList().add("badge primary");
        return badge;
    }
}

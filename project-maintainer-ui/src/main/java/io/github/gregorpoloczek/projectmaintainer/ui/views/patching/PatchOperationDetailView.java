package io.github.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.RemoteBranch;

public class PatchOperationDetailView extends VerticalLayout {

    protected Component toComponent(String text, RemoteBranch remoteBranch) {
        Component branchName = remoteBranch.getHref()
                .map(href -> {
                    Anchor anchor = new Anchor();
                    anchor.setHref(href);
                    anchor.setText(remoteBranch.getName());
                    anchor.setTarget("_blank");
                    return anchor;
                }).map(Component.class::cast)
                .orElseGet(() -> new Span(remoteBranch.getName()));

        return new HorizontalLayout(
                VaadinIcon.EXTERNAL_LINK.create(),
                new Span(text),
                branchName);
    }

    protected Component toComponent(String text, PullRequest pullRequest) {
        Anchor pullRequestTitle = new Anchor();
        pullRequestTitle.setHref(pullRequest.getBrowserLink());
        pullRequestTitle.setText(pullRequest.getTitle());
        pullRequestTitle.setTarget("_blank");

        return new HorizontalLayout(
                VaadinIcon.EXTERNAL_LINK.create(),
                new Span(text),
                pullRequestTitle);
    }

}

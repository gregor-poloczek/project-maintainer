package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.html.Div;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchStopResult;
import java.util.Optional;

public class PatchStopResultDetailView extends PatchOperationDetailView {

    public PatchStopResultDetailView(PatchStopResult result) {
        this.add(new Div(result.getDetail().getDescription()));
        switch (result.getDetail()) {
            case PatchStopResult.NoopResultDetail detail -> {
            }
            case PatchStopResult.DoneResultDetail detail -> {
                Optional<PullRequest> maybePullRequest = detail.getPullRequest();
                maybePullRequest.ifPresent(
                        pullRequest -> this.add(this.toComponent("Pull request", pullRequest)));
                this.add(this.toComponent("Remote branch", detail.getRemoteBranch()));
            }
            default -> this.add(new Div(result.getDetail().getName()) + " not implemented");
        }
        setSizeFull();
    }
}

package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.html.Div;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchStopResult;

public class PatchExecutionResultDetailView extends PatchOperationDetailView {

    public PatchExecutionResultDetailView(PatchExecutionResult patchExecutionResult) {
        this.add(new Div(patchExecutionResult.getDetail().getDescription()));
        switch (patchExecutionResult.getDetail()) {
            case PatchStopResult.NoopResultDetail _ -> {
            }
            case PatchExecutionResult.PreviewGeneratedResultDetail detail ->
                    this.add(new DiffComponent(detail.getUnifiedDiff()));
            case PatchExecutionResult.RemoteBranchExistsResultDetail detail -> this.add(this.toComponent(
                    "Remote Branch",
                    detail.getRemoteBranch()));
            case PatchExecutionResult.PullRequestStillOpenResultDetail detail -> {
                this.add(this.toComponent("Remote branch", detail.getRemoteBranch()));
                this.add(this.toComponent("Pull request", detail.getPullRequest()));
            }
            case PatchExecutionResult.AppliedResultDetail detail -> {
                PullRequest pullRequest = detail.getPullRequest();
                this.add(this.toComponent("Remote Branch", detail.getRemoteBranch()));
                this.add(this.toComponent("Pull request", pullRequest));
            }
            default -> this.add(new Div(patchExecutionResult.getDetail().getName() + " not implemented"));
        }
        setSizeFull();
    }

}

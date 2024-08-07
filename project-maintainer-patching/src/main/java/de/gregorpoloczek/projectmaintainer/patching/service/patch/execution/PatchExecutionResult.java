package de.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileOperation;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder(toBuilder = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PatchExecutionResult implements PatchOperationResult {

    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class NoopResultDetail implements PatchOperationResultDetail {

        private final String name = "No-Op";
        private final String description = "Patch did not change any files.";
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class PreviewGeneratedResultDetail implements PatchOperationResultDetail {

        private final String name = "Preview Generated";
        private final String description = "Preview of all projected changes generated.";

        String unifiedDiff;
        @NonNull
        List<ProjectFileOperation> operations;
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class AppliedResultDetail implements PatchOperationResultDetail {

        private final String name = "Patch applied";
        private final String description = "All projected changes were applied in a remote branch, and a pull request was created.";

        String commitMessage;
        RemoteBranch remoteBranch;
        PullRequest pullRequest;
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class PullRequestStillOpenResultDetail implements PatchOperationResultDetail {

        private final String name = "Open pull request found";
        private final String description = "A pull request for the patch is already open.";

        RemoteBranch remoteBranch;
        PullRequest pullRequest;
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class RemoteBranchExistsResultDetail implements PatchOperationResultDetail {

        private final String name = "Conflicting remote branch found";
        private final String description = "A conflicting remote branch with the same name was detected.";

        RemoteBranch remoteBranch;
    }

    @NonNull
    private PatchOperationResultDetail detail;

}

package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;

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

        @Override
        public Type getType() {
            return Type.NOOP;
        }
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class PreviewGeneratedResultDetail implements PatchOperationResultDetail {

        private final String name = "Preview Generated";
        private final String description = "Preview of all projected changes generated.";

        @NonNull
        UnifiedDiff unifiedDiff;

        @Override
        public Type getType() {
            return Type.PREVIEWED;
        }
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

        @Override
        public Type getType() {
            return Type.APPLIED;
        }
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

        @Override
        public Type getType() {
            return Type.BLOCKED;
        }
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class RemoteBranchExistsResultDetail implements PatchOperationResultDetail {

        private final String name = "Conflicting remote branch found";
        private final String description = "A conflicting remote branch with the same name was detected.";

        RemoteBranch remoteBranch;

        @Override
        public Type getType() {
            return Type.BLOCKED;
        }
    }

    @NonNull
    private PatchOperationResultDetail detail;

}

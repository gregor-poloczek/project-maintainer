package de.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder(toBuilder = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PatchStopResult implements PatchOperationResult {

    PatchOperationResultDetail detail;

    /**
     * No ongoing patch (neither branch nor pull request).
     */
    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class NoopResultDetail implements PatchOperationResultDetail {

        private final String description = "No pull request or existing patch remote branch was found.";

        private final String name = "No-Op";
    }

    /**
     * Pull request declined (if it existed) and branch deleted.
     */
    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class DoneResultDetail implements PatchOperationResultDetail {

        private final String description = "Remote branch was deleted, and pull request closed if an open one existed.";
        private final String name = "Done";

        PullRequest pullRequest;
        RemoteBranch remoteBranch;

        public Optional<PullRequest> getPullRequest() {
            return Optional.ofNullable(pullRequest);
        }

    }

}

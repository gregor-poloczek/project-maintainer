package de.gregorpoloczek.projectmaintainer.patching.service.patch.definition;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchMetaData {

    @NonNull
    String id;
    @NonNull
    String description;
    String commitPrefix;
    String commitMessage;
    String branchName;

    public Optional<String> getCommitPrefix() {
        return Optional.ofNullable(commitPrefix);
    }

    public Optional<String> getCommitMessage() {
        return Optional.ofNullable(commitMessage);
    }

    public Optional<String> getBranchName() {
        return Optional.ofNullable(branchName);
    }
}

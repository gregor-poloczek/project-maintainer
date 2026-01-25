package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchMetaData {

    @NonNull
    String id;
    @NonNull
    String description;
    String commitPrefix;
    String commitMessage;
    String branchName;

    @Builder.Default
    List<PatchParameter> patchParameters = new ArrayList<>();

    @Deprecated
    public Optional<String> getBranchName() {
        return Optional.ofNullable(branchName);
    }
}

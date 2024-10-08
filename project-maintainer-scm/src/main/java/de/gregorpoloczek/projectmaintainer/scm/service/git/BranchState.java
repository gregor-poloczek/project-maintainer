package de.gregorpoloczek.projectmaintainer.scm.service.git;

import java.util.SortedSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class BranchState {

    @NonNull
    SortedSet<String> remoteBranches;
    @NonNull
    SortedSet<String> localBranches;
    @NonNull
    String defaultBranch;
}

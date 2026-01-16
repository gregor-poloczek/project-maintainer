package de.gregorpoloczek.projectmaintainer.scm.service.git;

import java.util.SortedSet;

import lombok.NonNull;
import lombok.Value;

@Value
public class BranchState {

    @NonNull
    SortedSet<String> remoteBranches;
    @NonNull
    SortedSet<String> localBranches;
    @NonNull
    String defaultBranch;
}

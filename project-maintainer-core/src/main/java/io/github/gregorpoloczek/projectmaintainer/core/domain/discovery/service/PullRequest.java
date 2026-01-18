package io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

public interface PullRequest {

    String getTitle();

    String getSourceBranchName();

    String getTargetBranchName();

    String getBrowserLink();

    Object getId();
}

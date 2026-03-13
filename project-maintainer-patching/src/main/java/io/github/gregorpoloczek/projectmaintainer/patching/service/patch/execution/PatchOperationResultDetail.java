package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

public interface PatchOperationResultDetail {

    enum Type {
        NOOP,
        BLOCKED,
        PREVIEWED,
        PREVIEW_FAILED,
        APPLIED,
        APPLY_FAILED,
        STOPPED,
        STOP_FAILED
    }

    String getDescription();

    String getName();

    Type getType();
}

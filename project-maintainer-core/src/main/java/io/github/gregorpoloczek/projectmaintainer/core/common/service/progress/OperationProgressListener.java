package io.github.gregorpoloczek.projectmaintainer.core.common.service.progress;

@FunctionalInterface
public interface OperationProgressListener<T, P extends OperationProgress<T>> {

    void onProgress(P progress);
}

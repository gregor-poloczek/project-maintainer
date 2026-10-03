package io.github.gregorpoloczek.projectmaintainer.core.common.service.progress;

public interface ProjectOperationProgressListener<T> extends OperationProgressListener<T, ProjectOperationProgress<T>> {

    void onProgress(ProjectOperationProgress<T> progress);
}

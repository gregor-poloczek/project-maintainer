package de.gregorpoloczek.projectmaintainer.core.common.service.progress;


import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class GenericOperationProgress<T> extends AbstractOperationProgress<T> {

    @Builder
    public GenericOperationProgress(
            @NonNull State state,
            String message,
            T result,
            Throwable throwable,
            Integer progressCurrent, Integer progressTotal) {
        super(state, message, result, throwable, progressCurrent == null ? 0 : progressCurrent,
                progressTotal == null ? 1 : progressTotal);
    }
}

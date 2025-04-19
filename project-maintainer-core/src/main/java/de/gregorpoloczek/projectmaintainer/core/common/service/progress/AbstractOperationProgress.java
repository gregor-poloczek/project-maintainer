package de.gregorpoloczek.projectmaintainer.core.common.service.progress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class AbstractOperationProgress<T> implements OperationProgress<T> {

    @NonNull
    private State state;

    private String message;

    private T result;

    @NonNull
    private Integer progressCurrent;

    @NonNull
    private Integer progressTotal;

    public int getProgressCurrent() {
        return progressCurrent;
    }

    public int getProgressTotal() {
        return progressTotal;
    }

    public T getResult() {
        if (this.result == null) {
            throw new IllegalStateException("No result defined.");
        }
        return result;
    }

}

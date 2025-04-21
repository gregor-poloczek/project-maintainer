package de.gregorpoloczek.projectmaintainer.core.common.service.progress;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class AbstractOperationProgress<T> implements OperationProgress<T> {

    @NonNull
    protected State state;

    protected String message;

    protected T result;
    protected Throwable throwable;

    @NonNull
    protected Integer progressCurrent;

    @NonNull
    protected Integer progressTotal;

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

    public Optional<Throwable> getThrowable() {
        return Optional.ofNullable(throwable);
    }


}

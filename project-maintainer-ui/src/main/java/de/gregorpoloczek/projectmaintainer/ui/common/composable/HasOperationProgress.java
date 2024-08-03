package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HasOperationProgress {

    private OperationProgress<?> operationProgress;

    public Optional<OperationProgress<?>> getOperationProgress() {
        return Optional.ofNullable(operationProgress);
    }

    public static HasOperationProgress empty() {
        return HasOperationProgress.builder().build();
    }
}

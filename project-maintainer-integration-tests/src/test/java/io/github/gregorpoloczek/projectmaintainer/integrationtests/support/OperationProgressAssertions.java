package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class OperationProgressAssertions {

    /**
     * Waits for the given operation to finish and returns its final progress, which has to be in state
     * {@link OperationProgress.State#DONE}.
     */
    public static <T extends OperationProgress<?>> T requireDone(Flux<T> operation) {
        T progress = operation.blockLast();
        assertThat(progress).isNotNull();
        assertThat(progress.getState()).isEqualTo(OperationProgress.State.DONE);
        return progress;
    }
}

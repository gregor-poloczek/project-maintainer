package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgressListener;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    /**
     * Runs the given blocking operation and returns its final progress, which has to be in state
     * {@link OperationProgress.State#DONE}. Failures of the operation are propagated.
     */
    public static ProjectOperationProgress<Void> requireDone(Consumer<ProjectOperationProgressListener<Void>> operation) {
        List<ProjectOperationProgress<Void>> progress = new ArrayList<>();
        operation.accept(progress::add);
        assertThat(progress).isNotEmpty();
        assertThat(progress.getLast().getState()).isEqualTo(OperationProgress.State.DONE);
        return progress.getLast();
    }
}

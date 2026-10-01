package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResult;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResultDetail;
import lombok.experimental.UtilityClass;

import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class PatchOperationAssertions {

    /**
     * Returns the detail of the result of a finished patch operation, which has to be of the given type.
     */
    public static <D extends PatchOperationResultDetail> D requireDetail(
            OperationProgress<? extends PatchOperationResult> progress, Class<D> detailType) {
        PatchOperationResultDetail detail = progress.getResult().map(PatchOperationResult::getDetail).orElse(null);
        assertThat(detail).isInstanceOf(detailType);
        return detailType.cast(detail);
    }
}

package de.gregorpoloczek.projectmaintainer.core.common.service.progress;


import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
public class ProjectOperationProgress<T> extends AbstractOperationProgress<T> implements ProjectRelatable {

    @NonNull
    FQPN fqpn;

    public @NonNull FQPN getFQPN() {
        return fqpn;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(ToStringStyle.SIMPLE_STYLE)
                .append("fpqn", fqpn)
                .append("state", getState())
                .append("progress", getProgressCurrent() + "/" + getProgressTotal())
                .append("message", getMessage()).build();
    }

    @Builder
    public ProjectOperationProgress(
            @NonNull State state,
            String message,
            T result,
            Integer progressCurrent, Integer progressTotal,
            @NonNull FQPN fqpn) {
        super(state, message, result, progressCurrent == null ? 0 : progressCurrent,
                progressTotal == null ? 1 : progressTotal);
        this.fqpn = fqpn;
    }
}

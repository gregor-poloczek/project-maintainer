package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectAnalysisProgress {

    private final FQPN fqpn;
    @Builder.Default
    private int progressCurrent = 0;
    @Builder.Default
    private int progressTotal = 1;

    public FQPN getFQPN() {
        return fqpn;
    }

    private final State state;

    public enum State {
        SCHEDULED, RUNNING, DONE, FAILED;

        public boolean isTerminated() {
            return this == State.DONE || this == State.FAILED;
        }
    }

}

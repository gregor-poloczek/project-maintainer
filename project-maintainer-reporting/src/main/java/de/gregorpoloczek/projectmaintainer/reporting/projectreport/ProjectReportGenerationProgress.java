package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class ProjectReportGenerationProgress {

    private State state;
    @ToString.Exclude
    private ProjectReport projectReport;
    @Builder.Default
    private int progressCurrent = 0;
    @Builder.Default
    private int progressTotal = 1;

    public enum State {
        SCHEDULED, RUNNING, DONE, FAILED;

        public boolean isTerminated() {
            return this == State.DONE
                    || this == State.FAILED;
        }
    }
}

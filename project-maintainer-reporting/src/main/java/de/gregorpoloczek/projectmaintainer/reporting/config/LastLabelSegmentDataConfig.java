package de.gregorpoloczek.projectmaintainer.reporting.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LastLabelSegmentDataConfig extends DataConfig {

    @NotEmpty
    String base;

}

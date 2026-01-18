package io.github.gregorpoloczek.projectmaintainer.reporting.config;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportFile {

    @NotNull
    @Valid
    ReportConfig report;
}

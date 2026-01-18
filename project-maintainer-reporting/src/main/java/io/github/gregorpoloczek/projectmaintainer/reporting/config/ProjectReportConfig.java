package io.github.gregorpoloczek.projectmaintainer.reporting.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReportConfig extends ReportConfig {

    @NotNull
    @Valid
    List<ColumnConfig> columns = new ArrayList<>();

    List<String> requiredLabels;

    public Optional<List<String>> getRequiredLabels() {
        return Optional.ofNullable(requiredLabels);
    }

}

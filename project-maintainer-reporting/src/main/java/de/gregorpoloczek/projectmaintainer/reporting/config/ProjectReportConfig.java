package de.gregorpoloczek.projectmaintainer.reporting.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReportConfig extends ReportConfig {

    @NotEmpty
    @Valid
    List<ColumnConfig> columns = new ArrayList<>();

    List<String> requiredLabels;

    public Optional<List<String>> getRequiredLabels() {
        return Optional.ofNullable(requiredLabels);
    }

}

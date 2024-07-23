package de.gregorpoloczek.projectmaintainer.reporting.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
public class ProjectReportConfig extends ReportConfig {

    @NotEmpty
    List<ColumnConfig> columns = new ArrayList<>();

    List<String> requiredLabels;

    public Optional<List<String>> getRequiredLabels() {
        return Optional.ofNullable(requiredLabels);
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ColumnConfig {

        @NotEmpty
        String name;
        String labelPresence;
        String labelBase;
        String textAlignment = "left";
    }
}

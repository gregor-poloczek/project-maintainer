package de.gregorpoloczek.projectmaintainer.reporting.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
public class ProjectReportConfig extends ReportConfig {

    @NotEmpty
    List<ColumnConfig> columns = new ArrayList<>();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ColumnConfig {

        @NotEmpty
        String name;
        String versionedLabelBase;
    }
}

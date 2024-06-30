package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("project-maintainer.reporting")
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@Setter
public class ReportingProperties {

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Report {

        @NotEmpty
        String id;
        @NotEmpty
        String name;
        @NotEmpty
        List<Column> columns = new ArrayList<>();
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Column {

        @NotEmpty
        String name;
        String versionLabelBase;
    }

    List<Report> reports = new ArrayList<>();
}

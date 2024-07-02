package de.gregorpoloczek.projectmaintainer.reporting;

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
    public static class ReportProperties {

        @NotEmpty
        String id;
        @NotEmpty
        String name;
        @NotEmpty
        List<ColumnProperties> columns = new ArrayList<>();
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ColumnProperties {

        @NotEmpty
        String name;
        String versionLabelBase;
    }

    List<ReportProperties> reports = new ArrayList<>();
}

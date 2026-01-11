package de.gregorpoloczek.projectmaintainer.core.common.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties("project-maintainer")
@Component
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationProperties {
    Path workspacesDirectory = Path.of(System.getProperty("user.home"), ".project-maintainer", "workspaces");
}

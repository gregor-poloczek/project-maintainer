package io.github.gregorpoloczek.projectmaintainer.core.common.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

@Validated
@ConfigurationProperties("project-maintainer")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationProperties {
    Path workspacesDirectory = Path.of(System.getProperty("user.home"), ".project-maintainer", "workspaces");

    @NotEmpty
    @Size(min = 16, max = 256)
    String encryptionMasterKey = generateDeviceData();

    @SneakyThrows({IOException.class})
    private static @NonNull String generateDeviceData() {
        // determine inode from home directory (if possible)
        String iNodeData = Optional.ofNullable(Files.readAttributes(Paths.get(System.getProperty("user.home")), BasicFileAttributes.class).fileKey())
                .map(Object::toString)
                .orElse("N/A");

        return System.getProperty("os.name") + ":" +
                System.getProperty("os.arch") + ":" +
                System.getProperty("user.name") + ":" + iNodeData;
    }

}

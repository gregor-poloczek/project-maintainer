package de.gregorpoloczek.projectmaintainer.ui.common;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Service
public class ImageResolverService {

    @Builder
    @Getter
    public static class ImageFormat {

        private String extension;
        private String mimetype;
    }

    @Builder
    @Getter
    public static class Image {

        private ImageFormat format;
        private byte[] bytes;
    }

    public Optional<Image> getImage(String path, String name) {
        return Stream.of(
                        ImageFormat.builder().extension("svg").mimetype("image/svg+xml").build(),
                        ImageFormat.builder().extension("png").mimetype("image/x-png").build())
                .map(format -> {
                    URL resource = this.getClass().getClassLoader().getResource(
                            "de/gregorpoloczek/projectmaintainer/core/%s/%s.%s".formatted(path,
                                    name, format.getExtension()));
                    if (resource == null) {
                        return null;
                    }
                    byte[] bytes;
                    try {
                        bytes = IOUtils.toByteArray(resource);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    return Image.builder().format(format).bytes(bytes).build();
                })
                .filter(Objects::nonNull)
                .findFirst();
    }
}

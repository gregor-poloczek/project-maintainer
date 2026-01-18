package io.github.gregorpoloczek.projectmaintainer.ui.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageResolverService {

    private final WorkingCopyService workingCopyService;
    private final ProjectService projectService;

    public static final String GIT_PROVIDER_GROUP = "gitprovider";

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

    public Optional<Image> getProjectImage(ProjectRelatable relatable) {
        Optional<URI> maybeIcon = workingCopyService.find(relatable.getFQPN())
                .map(w -> w.getDirectory().toPath().resolve("./.idea/icon.svg").toFile())
                .filter(File::exists)
                .map(File::toURI);

        Optional<Image> image;
        if (maybeIcon.isPresent()) {
            try {
                image = Optional.of(Image.builder()
                        .format(ImageFormat.builder().mimetype("image/svg+xml").extension("svg").build())
                        .bytes(IOUtils.toByteArray(maybeIcon.get())).build());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            image = this.getImage(GIT_PROVIDER_GROUP,
                    projectService.require(relatable).requireFacet(BelongsToProjectConnection.class).getProjectConnection().getType());
        }
        return image;
    }

    public Optional<Image> getImage(String path, String name) {
        return Stream.of(
                        ImageFormat.builder().extension("svg").mimetype("image/svg+xml").build(),
                        ImageFormat.builder().extension("png").mimetype("image/x-png").build())
                .map(format -> {
                    URL resource = this.getClass().getClassLoader().getResource(
                            "io/github/gregorpoloczek/projectmaintainer/ui/%s/%s.%s".formatted(path,
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

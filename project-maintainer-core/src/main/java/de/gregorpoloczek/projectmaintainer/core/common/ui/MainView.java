package de.gregorpoloczek.projectmaintainer.core.common.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.GitProvider;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.swing.text.html.Option;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

@Route
public class MainView extends VerticalLayout {

    public final LitRenderer<Project> iconRenderer =
            LitRenderer.<Project>of("<img src=${item.image} style=\"height:32px;\" />")
                    .withProperty("image", project -> {
                        Optional<Image> image = MainView.this.getImage("gitprovider",
                                project.getMetaData().getGitProvider()
                                        .name());

                        return image.map(i -> "data:" + i.getFormat().getMimetype() + ";base64," + Base64.getEncoder()
                                .encodeToString(i.getBytes())).orElse("");
                    });


    @Builder
    @Getter
    private static class ImageFormat {

        private String extension;
        private String mimetype;
    }

    @Builder
    @Getter
    private static class Image {

        private ImageFormat format;
        private byte[] bytes;
    }

    private Optional<Image> getImage(String path, String name) {
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

    private final ProjectService projectService;
    private final Grid<Project> grid;

    public MainView(ProjectService projectService) {
        this.projectService = projectService;
        grid = new Grid<>(Project.class, false);
        grid.addColumn(this.iconRenderer).setFlexGrow(0);
        grid.addColumn(p -> p.getMetaData().getName()).setHeader("Name");
        this.add(grid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        grid.setItems(projectService.getProjects());
    }
}

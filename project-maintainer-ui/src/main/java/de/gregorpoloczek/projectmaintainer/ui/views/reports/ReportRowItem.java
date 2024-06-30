package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasIconItem;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasProjectItem;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportRowItem implements HasIconItem, HasProjectItem {

    @Getter
    private Project project;
    private String[] values;

    public ReportRowItem(Project project, int columnsCount) {
        this.values = new String[columnsCount];
        this.project = project;
    }

    @Override
    public boolean isIconBlurred() {
        return false;
    }

    @Override
    public Optional<Image> getIcon() {
        return Optional.empty();
    }

    @Getter
    @Setter
    boolean render;

    public void setValue(int index, String version) {
        this.values[index] = version;
    }

    public Optional<String> getValue(int index) {
        return Optional.ofNullable(this.values[index]);
    }
}

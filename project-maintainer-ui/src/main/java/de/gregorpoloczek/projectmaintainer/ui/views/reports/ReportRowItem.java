package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
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
    @Getter
    private Optional<Image> icon;
    private ReportCellValue[] values;

    public ReportRowItem(Project project, int columnsCount, Optional<Image> icon) {
        this.project = project;
        this.values = new ReportCellValue[columnsCount];
        this.icon = icon;
    }

    @Override
    public boolean isIconBlurred() {
        return false;
    }

    @Getter
    @Setter
    boolean render;

    public void setValue(int index, ReportCellValue value) {
        this.values[index] = value;
    }

    public Optional<ReportCellValue> getValue(int index) {
        return Optional.ofNullable(this.values[index]);
    }
}

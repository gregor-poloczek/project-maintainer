package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportRow extends AbstractComposable<ReportRow> {

    private ReportCellValue[] values;

    public ReportRow(int columnsCount) {
        this.values = new ReportCellValue[columnsCount];
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

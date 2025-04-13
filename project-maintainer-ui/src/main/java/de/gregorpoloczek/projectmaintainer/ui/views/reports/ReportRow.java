package de.gregorpoloczek.projectmaintainer.ui.views.reports;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportRow extends AbstractComposable<FQPN, ReportRow> {

    final FQPN fqpn;
    private ReportCellValue[] values;

    public ReportRow(FQPN fqpn, int columnsCount) {
        this.fqpn = fqpn;
        this.values = new ReportCellValue[columnsCount];
    }


    @Override
    public FQPN getKey() {
        return fqpn;
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

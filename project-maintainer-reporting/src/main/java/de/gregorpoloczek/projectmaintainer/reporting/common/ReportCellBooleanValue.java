package de.gregorpoloczek.projectmaintainer.reporting.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ReportCellBooleanValue implements ReportCellValue {

    boolean booleanValue;

    public static ReportCellValue of(boolean booleanValue) {
        return new ReportCellBooleanValue(booleanValue);
    }

    @Override
    public String getStringValue() {
        return Boolean.toString(booleanValue);
    }
}

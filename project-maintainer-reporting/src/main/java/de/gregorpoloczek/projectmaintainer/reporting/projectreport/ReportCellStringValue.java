package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ReportCellStringValue implements ReportCellValue {

    String stringValue;

    public static ReportCellStringValue of(String stringValue) {
        return new ReportCellStringValue(stringValue);
    }
}

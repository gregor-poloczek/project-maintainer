package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ReportCellErrorValue implements ReportCellValue {

    String stringValue;

    public static ReportCellErrorValue of(String stringValue) {
        return new ReportCellErrorValue(stringValue);
    }
}

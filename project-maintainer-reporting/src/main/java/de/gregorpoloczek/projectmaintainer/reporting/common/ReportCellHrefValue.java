package de.gregorpoloczek.projectmaintainer.reporting.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ReportCellHrefValue implements ReportCellValue {

    String text;
    String href;

    @Override
    public String getStringValue() {
        return this.text;
    }

    public static ReportCellHrefValue of(String text, String href) {
        return new ReportCellHrefValue(text, href);
    }
}

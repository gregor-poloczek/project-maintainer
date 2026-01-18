package io.github.gregorpoloczek.projectmaintainer.reporting.common;

import io.github.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
public class ReportColumn {

    final ColumnTextAlignment textAlignment;
    final String label;
}

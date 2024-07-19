package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
public class ProjectReportColumn {

    final ColumnTextAlignment textAlignment;
    final String label;
}

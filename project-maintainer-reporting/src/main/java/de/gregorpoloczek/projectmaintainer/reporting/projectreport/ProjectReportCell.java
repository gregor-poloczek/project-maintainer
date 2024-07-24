package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class ProjectReportCell {

    final ReportCellValue value;
}

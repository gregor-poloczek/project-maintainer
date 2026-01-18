package io.github.gregorpoloczek.projectmaintainer.reporting.projectreport;

import io.github.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class ProjectReportCell {

    final ReportCellValue value;
}

package io.github.gregorpoloczek.projectmaintainer.reporting.spi;

import io.github.gregorpoloczek.projectmaintainer.reporting.config.ReportConfig;

import java.util.List;

public interface ReportDefinitionProvider {
    List<ReportConfig> create();
}

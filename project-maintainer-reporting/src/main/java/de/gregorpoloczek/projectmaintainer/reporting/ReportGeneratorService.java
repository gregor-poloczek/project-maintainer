package de.gregorpoloczek.projectmaintainer.reporting;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportGenerationProgress;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportGenerationProgress.State;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReport;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportCell;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportColumn;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportDefinition;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportRow;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig.ColumnConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportFile;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellBooleanValue;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellErrorValue;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellStringValue;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class ReportGeneratorService {

    private final LabelService labelService;
    private final WorkingCopyService workingCopyService;
    private final ProjectService projectService;
    private final ProjectAnalysisService projectAnalysisService;

    @Value("file:./.reports/*.yml")
    private Resource[] reportFiles;

    public ReportGeneratorService(
            LabelService labelService,
            WorkingCopyService workingCopyService,
            ProjectService projectService,
            ProjectAnalysisService projectAnalysisService
    ) {
        this.labelService = labelService;
        this.workingCopyService = workingCopyService;
        this.projectService = projectService;
        this.projectAnalysisService = projectAnalysisService;
    }

    private SortedMap<String, ReportConfig> reportConfigs = Collections.synchronizedSortedMap(new TreeMap<>());

    @PostConstruct
    void init() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(
                        PropertyNamingStrategies.KEBAB_CASE);
        for (Resource resource : reportFiles) {
            try {
                ReportFile reportFile =
                        objectMapper.readValue(resource.getURL(),
                                ReportFile.class);

                String reportId = FilenameUtils.removeExtension(resource.getFile().getName());
                reportFile.getReport().setId(reportId);
                reportConfigs.put(reportId, reportFile.getReport());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public List<ProjectReportConfig> getProjectReportConfigs() {
        return this.reportConfigs.values().stream()
                .filter(ProjectReportConfig.class::isInstance)
                .map(ProjectReportConfig.class::cast)
                .collect(toList());
    }

    public Flux<ProjectReportGenerationProgress> generateProjectReport(String reportId) {
        ReportConfig reportConfig = this.reportConfigs.get(reportId);

        if (reportConfig == null) {
            return Flux.error(new IllegalArgumentException(
                    "Cannot find report definition with id " + reportId));
        }
        if (!(reportConfig instanceof ProjectReportConfig)) {
            return Flux.error(new IllegalArgumentException(
                    "Report definition is not of type project-report"));
        }

        List<Project> projects = projectService.getProjects()
                .stream()
                .filter(p -> workingCopyService.find(p).isPresent()).toList();

        ProjectReport report = this.buildReport((ProjectReportConfig) reportConfig);

        return Flux.create(sink -> {
            sink.next(ProjectReportGenerationProgress.builder()
                    .projectReport(report)
                    .state(State.SCHEDULED).build());

            AtomicInteger analyzed = new AtomicInteger(0);

            Flux.fromIterable(projects)
                    .flatMap(p -> projectAnalysisService
                            .analyze(p)
                            .subscribeOn(Schedulers.parallel())
                            .last()
                            .thenReturn(p))
                    .doOnNext(project -> {
                        addToReport(report, (ProjectReportConfig) reportConfig, project);
                        sink.next(ProjectReportGenerationProgress.builder()
                                .projectReport(report)
                                .state(State.RUNNING)
                                .progressTotal(projects.size())
                                .progressCurrent(analyzed.incrementAndGet())
                                .build());
                    })
                    .doOnComplete(() -> {
                        sink.next(ProjectReportGenerationProgress.builder()
                                .projectReport(report)
                                .state(State.DONE)
                                .progressCurrent(1)
                                .progressTotal(1)
                                .build());
                        sink.complete();
                    })
                    .doOnError(sink::error)
                    .subscribe(c -> {
                    });

        });


    }

    private void addToReport(ProjectReport report, ProjectReportConfig reportConfig, Project project) {
        ProjectReportRow row = generateRow(reportConfig, project);

        boolean addRow;
        Optional<List<String>> requiredLabels = reportConfig.getRequiredLabels();
        if (requiredLabels.isPresent()) {
            // row is visible, if it matches the required labels
            addRow = labelService.hasLabelsMatchingAll(project, requiredLabels.get());
        } else {
            // default case: row is visible only at least cell has a value
            addRow = row.getCells().stream().anyMatch(c -> c.getValue() != null);
        }

        if (addRow) {
            report.getRows().add(row);
            report.getRows().sort(Comparator.comparing(r -> r.getProject().getFQPN()));
        }
    }

    private ProjectReportRow generateRow(ProjectReportConfig reportConfig, Project project) {
        ProjectReportRow row = new ProjectReportRow(project);
        for (ColumnConfig column : reportConfig.getColumns()) {
            Optional<ReportCellValue> value = generateReportCellValue(project, column);

            row.getCells().add(ProjectReportCell.builder().value(value.orElse(null)).build());
        }
        return row;
    }

    private Optional<ReportCellValue> generateReportCellValue(Project project, ColumnConfig column) {
        Optional<ReportCellValue> value;
        if (column.getLabelBase() != null) {
            value = this.labelService.findLabelsByBase(project, Label.fromString(column.getLabelBase()))
                    .stream()
                    .findFirst()
                    .map(Label::getLastSegment)
                    .map(ReportCellStringValue::of);
        } else if (column.getLabelPresence() != null) {
            boolean hasLabel = this.labelService.hasLabel(project, Label.fromString(column.getLabelPresence()));
            value = Optional.of(ReportCellBooleanValue.of(hasLabel));
        } else {
            value = Optional.of(ReportCellErrorValue.of("??column-type??"));
        }
        return value;
    }

    private ProjectReport buildReport(ProjectReportConfig reportConfig) {
        ProjectReportDefinition reportDefinition = new ProjectReportDefinition();
        reportDefinition.setId(reportConfig.getId());
        reportDefinition.setName(reportConfig.getName());
        reportDefinition.setColumns(reportConfig.getColumns()
                .stream()
                .map(c -> ProjectReportColumn.builder()
                        .label(c.getName())
                        .textAlignment(ColumnTextAlignment.fromString(c.getTextAlignment()))
                        .build())
                .toList());

        ProjectReport report = new ProjectReport();
        report.setDefinition(reportDefinition);
        return report;
    }


}

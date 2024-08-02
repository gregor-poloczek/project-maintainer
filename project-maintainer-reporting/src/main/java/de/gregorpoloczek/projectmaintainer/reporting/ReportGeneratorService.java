package de.gregorpoloczek.projectmaintainer.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.analysis.fulltext.ProjectFullTextSearchService;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.GenericOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.git.service.workingcopy.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ColumnTextAlignment;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReport;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportCell;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportColumn;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportDefinition;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportRow;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ColumnConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportFile;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellBooleanValue;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellErrorValue;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellStringValue;
import de.gregorpoloczek.projectmaintainer.reporting.common.ReportCellValue;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGeneratorService {

    private final LabelService labelService;
    private final WorkingCopyService workingCopyService;
    private final ProjectService projectService;
    private final ProjectAnalysisService projectAnalysisService;
    private final ProjectFullTextSearchService projectFullTextSearchService;


    @Value("file:./.reports/*.yml")
    private Resource[] reportFiles;

    private final SortedMap<String, ReportConfig> reportConfigs =
            Collections.synchronizedSortedMap(new TreeMap<>());

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

                try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
                    Validator validator = factory.getValidator();

                    Set<ConstraintViolation<ReportFile>> violations = validator.validate(reportFile);
                    if (!violations.isEmpty()) {
                        String text = violations.stream()
                                .map(v -> "* " +
                                        v.getPropertyPath() + ": " + v.getInvalidValue() + " -> " + v.getMessage())
                                .collect(Collectors.joining("\n"));
                        throw new IllegalStateException(
                                "Yaml \"%s\" is invalid:%n%s".formatted(resource.getFilename(), text));
                    }
                }

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
                .toList();
    }

    public Flux<GenericOperationProgress<ProjectReport>> generateProjectReport(String reportId) {
        ReportConfig reportConfig = this.reportConfigs.get(reportId);

        if (reportConfig == null) {
            return Flux.error(new IllegalArgumentException(
                    "Cannot find report definition with id " + reportId));
        }
        if (!(reportConfig instanceof ProjectReportConfig)) {
            return Flux.error(new IllegalArgumentException(
                    "Report definition is not of type project-report"));
        }

        List<Project> projects = projectService.findALl()
                .stream()
                .filter(p -> workingCopyService.find(p).isPresent()).toList();

        ProjectReport report = this.buildReport((ProjectReportConfig) reportConfig);

        return Flux.create(sink -> {
            sink.next(GenericOperationProgress.<ProjectReport>builder()
                    .result(report)
                    .state(OperationProgress.State.SCHEDULED).build());

            AtomicInteger analyzed = new AtomicInteger(0);
            Disposable subscription = Flux.fromIterable(projects)
                    .flatMap(p -> projectAnalysisService
                            .analyze(p)
                            .subscribeOn(Schedulers.parallel())
                            .last()
                            .thenReturn(p))
                    .doOnNext(project -> {
                        addToReport(report, (ProjectReportConfig) reportConfig, project);
                        sink.next(GenericOperationProgress.<ProjectReport>builder()
                                .result(report)
                                .state(OperationProgress.State.RUNNING)
                                .progressTotal(projects.size())
                                .progressCurrent(analyzed.incrementAndGet())
                                .build());
                    })
                    .doOnComplete(() -> {
                        sink.next(GenericOperationProgress.<ProjectReport>builder()
                                .result(report)
                                .state(OperationProgress.State.DONE)
                                .progressCurrent(1)
                                .progressTotal(1)
                                .build());
                        sink.complete();
                    })
                    .doOnError(sink::error)
                    .subscribe();

            // TODO ist das korrekt?
            sink.onDispose(subscription);
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
            List<ProjectReportRow> rows = new ArrayList<>(report.getRows());
            rows.add(row);
            rows.sort(Comparator.comparing(r -> r.getProject().getFQPN()));
            report.setRows(rows);
        }
    }

    private ProjectReportRow generateRow(ProjectReportConfig reportConfig, Project project) {
        ProjectReportRow row = new ProjectReportRow(project);
        for (ColumnConfig column : reportConfig.getColumns()) {
            ReportCellValue value = generateReportCellValue(project, column);
            ProjectReportCell cell = ProjectReportCell.builder().value(value).build();
            row.getCells().add(cell);
        }
        return row;
    }

    private ReportCellValue generateReportCellValue(Project project, ColumnConfig column) {
        // TODO multiple values per cell

        ReportCellValue value;
        if (column.getFilePresence() != null) {
            // TODO with text optional machen
            List<ProjectFileLocation> textMatches = projectFullTextSearchService.search(project,
                    column.getFilePresence().getFile(),
                    "\"" + column.getFilePresence().getWithText() + "\"");

            List<ProjectFileLocation> fileMatches = projectFullTextSearchService.search(project,
                    column.getFilePresence().getFile());

            // TODO das pfad filtering über den index machen (path steht bereits im document)
            value = textMatches.stream()
                    .findFirst()
                    .map(location -> ReportCellBooleanValue.builder().booleanValue(true).location(location).build())
                    .orElseGet(() -> fileMatches.stream()
                            .findFirst()
                            .map(location -> ReportCellBooleanValue.builder()
                                    .booleanValue(false)
                                    .location(location)
                                    .build())
                            .orElseGet(() -> ReportCellBooleanValue.builder().booleanValue(false).build()));
        } else if (column.getLabelBase() != null) {
            value = this.labelService.findLabelsByBase(project, Label.fromString(column.getLabelBase()))
                    .stream()
                    .findFirst()
                    .map(label -> {
                        if (label.getLocation().isPresent()) {
                            return ReportCellStringValue.builder()
                                    .stringValue(label.getLastSegment())
                                    .location(label.getLocation().get())
                                    .build();
                        } else {
                            return ReportCellStringValue.builder().stringValue(label.getLastSegment()).build();
                        }
                    }).orElse(null);
        } else if (column.getLabelPresence() != null) {
            boolean hasLabel = this.labelService.hasLabel(project, Label.fromString(column.getLabelPresence()));
            value = ReportCellBooleanValue.builder().booleanValue(hasLabel).build();
        } else {
            value = ReportCellErrorValue.builder().stringValue("??column-type??").build();
        }
        return value;
    }

    private ProjectReport buildReport(ProjectReportConfig reportConfig) {
        ProjectReportDefinition reportDefinition = new ProjectReportDefinition();
        reportDefinition.setId(reportConfig.getId());
        reportDefinition.setName(reportConfig.getName());
        reportDefinition.setColumns(reportConfig.getColumns()
                .stream()
                .map(c -> ReportColumn.builder()
                        .label(c.getName())
                        .textAlignment(ColumnTextAlignment.fromString(c.getTextAlignment()))
                        .build())
                .toList());

        ProjectReport report = new ProjectReport();
        report.setDefinition(reportDefinition);
        return report;
    }


}

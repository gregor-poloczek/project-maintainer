package de.gregorpoloczek.projectmaintainer.reporting;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gregorpoloczek.projectmaintainer.analysis.Label;
import de.gregorpoloczek.projectmaintainer.analysis.LabelService;
import de.gregorpoloczek.projectmaintainer.analysis.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.analysis.VersionedLabel;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.reporting.ReportGeneratorService.ProjectReportGenerationProgress.State;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReport;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportCell;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportColumn;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportDefinition;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.ProjectReportRow;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ProjectReportConfig.ColumnConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.config.ReportFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
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

    @Builder
    @Getter
    @ToString
    public static class ProjectReportGenerationProgress {

        private State state;
        @ToString.Exclude
        private ProjectReport projectReport;
        @Builder.Default
        private int progressCurrent = 0;
        @Builder.Default
        private int progressTotal = 1;

        public enum State {
            SCHEDULED, RUNNING, DONE, FAILED;

            public boolean isTerminated() {
                return this == ProjectReportGenerationProgress.State.DONE
                        || this == ProjectReportGenerationProgress.State.FAILED;
            }
        }

        public ProjectReport getProjectReport() {
            if (this.state != State.DONE) {
                throw new IllegalStateException("Project not generated");
            }
            return this.projectReport;
        }
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
            sink.next(ProjectReportGenerationProgress.builder().state(State.SCHEDULED).build());

            AtomicInteger analyzed = new AtomicInteger(0);
            Flux.merge(projects
                            .stream()
                            .map(p -> projectAnalysisService
                                    .analyze(p)
                                    .subscribeOn(Schedulers.parallel())
                                    .last()
                                    .thenReturn(p))
                            .toList())
                    .doOnNext(project -> {
                        addToReport(report, (ProjectReportConfig) reportConfig, project);
                        sink.next(ProjectReportGenerationProgress.builder()
                                .progressTotal(projects.size())
                                .progressCurrent(analyzed.incrementAndGet())
                                .state(State.RUNNING).build());
                    }).doOnComplete(() -> {
                        sink.next(ProjectReportGenerationProgress.builder()
                                .state(State.DONE)
                                .progressCurrent(1)
                                .progressTotal(1)
                                .projectReport(report)
                                .build());
                        sink.complete();
                    }).subscribe(c -> {
                    });

        });


    }

    private void addToReport(ProjectReport report, ProjectReportConfig reportConfig, Project project) {
        ProjectReportRow row = new ProjectReportRow(project);
        SortedSet<Label> labels = this.labelService.find(project);

        for (ColumnConfig column : reportConfig.getColumns()) {
            Label label = Label.of(column.getVersionLabelBase());
            Optional<VersionedLabel> match = labels.stream()
                    .filter(VersionedLabel.class::isInstance)
                    .map(VersionedLabel.class::cast)
                    .filter(vL -> vL.getBase().equals(label))
                    .findFirst();

            ProjectReportCell cell = new ProjectReportCell();
            if (match.isPresent()) {
                cell.setValue(match.get().getVersion());
            } else {
                cell.setValue(null);
            }
            row.getCells().add(cell);
        }

        if (!row.getCells().stream().allMatch(c -> c.getValue() == null)) {
            report.getRows().add(row);
            report.getRows().sort(Comparator.comparing(r -> r.getProject().getFQPN()));
        }
    }

    private ProjectReport buildReport(ProjectReportConfig reportConfig) {
        ProjectReportDefinition reportDefinition = new ProjectReportDefinition();
        reportDefinition.setId(reportConfig.getId());
        reportDefinition.setName(reportConfig.getName());
        reportDefinition.setColumns(reportConfig.getColumns()
                .stream()
                .map(c -> ProjectReportColumn.builder().label(c.getName()).build())
                .toList());

        ProjectReport report = new ProjectReport();
        report.setDefinition(reportDefinition);
        return report;
    }


}

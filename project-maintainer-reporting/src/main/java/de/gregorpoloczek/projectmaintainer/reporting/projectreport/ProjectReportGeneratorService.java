package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

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
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.config.ProjectReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.config.ProjectReportConfig.ColumnConfig;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.config.ReportConfig;
import de.gregorpoloczek.projectmaintainer.reporting.projectreport.config.ReportFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ProjectReportGeneratorService {

    private final LabelService labelService;
    private final WorkingCopyService workingCopyService;
    private final ProjectService projectService;
    private final ProjectAnalysisService projectAnalysisService;

    @Value("file:./.reports/*.yml")
    private Resource[] reportFiles;

    public ProjectReportGeneratorService(
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

    public Mono<ProjectReport> generateProjectReport(String reportId) {
        ReportConfig reportConfig = this.reportConfigs.get(reportId);

        if (reportConfig == null) {
            return Mono.error(new IllegalArgumentException(
                    "Cannot find report definition with id " + reportId));
        }
        if (!(reportConfig instanceof ProjectReportConfig)) {
            return Mono.error(new IllegalArgumentException(
                    "Report definition is not of type project-report"));
        }

        Flux<Project> analyzedProjects = Flux.merge(projectService.getProjects()
                .stream()
                .filter(p -> workingCopyService.find(p.getMetaData().getFQPN()).isPresent())
                .map(p -> projectAnalysisService.analyze(p.getMetaData().getFQPN())
                        .subscribeOn(Schedulers.parallel()).thenReturn(p))
                .toList());
        return analyzedProjects
                .collectList()
                .map(projects -> buildReport(projects, (ProjectReportConfig) reportConfig));
    }

    private ProjectReport buildReport(List<Project> projects, ProjectReportConfig reportConfig) {
        ProjectReportDefinition reportDefinition = new ProjectReportDefinition();
        reportDefinition.id = reportConfig.getId();
        reportDefinition.name = reportConfig.getName();
        reportDefinition.columns = reportConfig.getColumns()
                .stream()
                .map(c -> ProjectReportColumn.builder().label(c.getName()).build())
                .toList();

        ProjectReport report = new ProjectReport();
        report.definition = reportDefinition;

        for (Project project : projects) {
            ProjectReportRow row = new ProjectReportRow(project);
            SortedSet<Label> labels = this.labelService.find(project.getMetaData().getFQPN());

            for (ColumnConfig column : reportConfig.getColumns()) {
                Label label = Label.of(column.getVersionLabelBase());
                Optional<VersionedLabel> match = labels.stream()
                        .filter(VersionedLabel.class::isInstance)
                        .map(VersionedLabel.class::cast)
                        .filter(vL -> vL.getBase().equals(label))
                        .findFirst();

                ProjectReportCell cell = new ProjectReportCell();
                if (match.isPresent()) {
                    cell.value = match.get().getVersion();
                } else {
                    cell.value = null;
                }
                row.cells.add(cell);
            }

            if (!row.cells.stream().allMatch(c -> c.value == null)) {
                report.rows.add(row);
            }
        }

        return report;
    }


}

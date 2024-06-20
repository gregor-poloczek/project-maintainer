package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.ProjectAnalysisService;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.OperationExecutionService;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.WorkingCopyService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationState;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import de.gregorpoloczek.projectmaintainer.ui.views.git.ProjectItem;
import java.util.List;
import java.util.Optional;

@Route
public class AnalysisView extends VerticalLayout {

    private final ProjectAnalysisService projectAnalysisService;
    private final ProjectService projectService;
    private final WorkingCopyService workingCopyService;
    private final OperationExecutionService operationExecutionService;

    public AnalysisView(
            ProjectAnalysisService projectAnalysisService,
            ProjectService projectService,
            WorkingCopyService workingCopyService,
            OperationExecutionService operationExecutionService) {
        this.projectAnalysisService = projectAnalysisService;
        this.projectService = projectService;
        this.workingCopyService = workingCopyService;
        this.operationExecutionService = operationExecutionService;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<Project> projects = this.projectService.getProjects();
        UI ui = UI.getCurrent();
        for (Project project : projects) {
            Optional<WorkingCopy> workingCopy = this.workingCopyService.find(project.getMetaData().getFQPN());

            if (workingCopy.isPresent()) {
                this.operationExecutionService.executeAsyncOperation2(
                                project,
                                "analysis::analyze",
                                this.projectAnalysisService::analyze)
                        .subscribe(e -> onUpdateEvent(e, ui));
            }
        }
    }

    private void onUpdateEvent(ProjectOperationProgress e, UI current) {
        if (!current.isAttached()) {
            // browser has been reloaded or closed in the mean time
            return;
        }
        current.access(() -> {
            System.out.println("Analysis progress: " + e.getProgress());
        });
    }

}

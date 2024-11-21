package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import de.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasLabels;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectAnalysis extends AbstractComposable<ProjectAnalysis> {

    public String getName() {
        return this.getProject().getMetaData().getName();
    }

    private Project getProject() {
        return this.requireTrait(HasProject.class).getProject();
    }


    public boolean matches(String query) {
        boolean name = this.getName().toLowerCase().contains(query);

        if (name) {
            return true;
        }
        // TODO label matching with regexp

        return this.requireTrait(HasLabels.class)
                .getLabels()
                .stream()
                .anyMatch(l -> l.getValue().toLowerCase().contains(query));
    }
}

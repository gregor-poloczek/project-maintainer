package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import de.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasIconItem;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasLabelsItem;
import de.gregorpoloczek.projectmaintainer.ui.common.HasProject;
import java.util.Optional;
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
public class ProjectAnalysisItem extends AbstractComposable<ProjectAnalysisItem> implements HasIconItem, HasLabelsItem {

    @Builder.Default
    SortedSet<Label> labels = new TreeSet<>();
    Optional<Image> icon;

    public String getName() {
        return this.getProject().getMetaData().getName();
    }

    private Project getProject() {
        return this.requireComponent(HasProject.class).getProject();
    }


    @Override
    public boolean isIconBlurred() {
        return false;
    }

    public boolean matches(String query) {
        boolean name = this.getName().toLowerCase().contains(query);

        if (name) {
            return true;
        }
        // TODO label matching with regexp

        return this.getLabels().stream().anyMatch(l -> l.getValue().toLowerCase().contains(query));
    }
}

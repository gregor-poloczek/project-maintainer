package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasIconItem;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasLabelsItem;
import de.gregorpoloczek.projectmaintainer.ui.common.Renderers.HasProjectItem;
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
public class ProjectAnalysisItem implements HasProjectItem, HasIconItem, HasLabelsItem {

    Project project;
    @Builder.Default
    SortedSet<Label> labels = new TreeSet<>();
    Optional<Image> icon;

    public String getName() {
        return this.getProject().getMetaData().getName();
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

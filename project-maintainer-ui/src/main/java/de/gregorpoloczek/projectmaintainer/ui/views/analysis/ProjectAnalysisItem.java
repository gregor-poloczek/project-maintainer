package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectAnalysisItem extends AbstractComposable<ProjectAnalysisItem> {

}

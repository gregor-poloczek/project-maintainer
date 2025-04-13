package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ProjectAnalysisItem extends AbstractComposable<FQPN, ProjectAnalysisItem> {

    FQPN fqpn;

    @Override
    public FQPN getKey() {
        return fqpn;
    }
}

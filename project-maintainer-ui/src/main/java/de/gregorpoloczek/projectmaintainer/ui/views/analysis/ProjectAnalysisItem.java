package de.gregorpoloczek.projectmaintainer.ui.views.analysis;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectAnalysisItem extends AbstractComposable<FQPN, ProjectAnalysisItem> {

    @NonNull
    FQPN fqpn;

    @Override
    public FQPN getKey() {
        return fqpn;
    }
}

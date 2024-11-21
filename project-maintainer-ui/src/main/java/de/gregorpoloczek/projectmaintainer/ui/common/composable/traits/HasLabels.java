package de.gregorpoloczek.projectmaintainer.ui.common.composable.traits;

import de.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HasLabels {

    Collection<Label> labels;
}

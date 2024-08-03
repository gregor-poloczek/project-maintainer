package de.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder(toBuilder = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HasWorkingCopy {

    WorkingCopy workingCopy;

    public Optional<WorkingCopy> getWorkingCopy() {
        return Optional.ofNullable(workingCopy);
    }
}

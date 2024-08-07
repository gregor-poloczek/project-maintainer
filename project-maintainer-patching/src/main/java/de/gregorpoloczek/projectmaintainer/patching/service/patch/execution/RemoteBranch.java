package de.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RemoteBranch {

    public Optional<String> getHref() {
        return Optional.ofNullable(href);
    }

    String href;
    @NonNull
    String name;
}

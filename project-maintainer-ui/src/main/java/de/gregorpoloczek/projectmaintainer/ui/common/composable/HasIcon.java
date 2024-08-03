package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService.Image;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HasIcon {

    public boolean isBlurred() {
        return blurred;
    }

    Image icon;

    @NonNull
    @Builder.Default
    Boolean blurred = false;

    public Optional<Image> getIcon() {
        return Optional.ofNullable(icon);
    }
}

package io.github.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class ProjectConnectionForm<T extends ProjectConnection> {

    public enum State {
        NEW, SAVED, DIRTY
    }

    @EqualsAndHashCode.Include
    @NotNull
    String id;
    @NotNull
    String type;
    @NotNull
    State state;
    @Nullable
    T connection;

    boolean valid;
}

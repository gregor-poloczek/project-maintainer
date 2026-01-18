package io.github.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common;

import com.vaadin.flow.component.Component;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import jakarta.validation.constraints.NotNull;

public interface ProjectConnectionUIAdapter<CNC extends ProjectConnection, CMP extends Component & ProjectConnectionFormComponent<CNC>> {
    @NotNull
    String getType();

    @NotNull
    CMP createComponent();

    default boolean supports(String type) {
        return getType().equals(type);
    }

    @NotNull
    String getTitle();

}

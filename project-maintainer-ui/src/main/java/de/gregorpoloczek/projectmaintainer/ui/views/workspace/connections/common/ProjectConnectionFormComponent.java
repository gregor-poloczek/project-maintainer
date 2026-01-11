package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common;

import com.vaadin.flow.component.HasValue;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import org.apache.commons.lang3.NotImplementedException;

public interface ProjectConnectionFormComponent<T extends ProjectConnection> extends HasValue<HasValue.ValueChangeEvent<ProjectConnectionForm<T>>, ProjectConnectionForm<T>> {


    @Override
    default void setReadOnly(boolean readOnly) {
        throw new NotImplementedException();
    }

    @Override
    default boolean isReadOnly() {
        throw new NotImplementedException();
    }

    @Override
    default void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        throw new NotImplementedException();
    }

    @Override
    default boolean isRequiredIndicatorVisible() {
        throw new NotImplementedException();
    }
}

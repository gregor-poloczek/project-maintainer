package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;

public class HasProjectFilterComponent<K, T extends AbstractComposable<K, T>> extends HorizontalLayout {

    private final TextField textField;

    public HasProjectFilterComponent(ComposableFilterSearch<K, T> composableFilterSearch) {
        textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setTooltipText("Project filer");
        setDecorated(true);
        this.add(textField);

        // participate in search
        var handle = composableFilterSearch.add(item -> {
            String value = textField.getValue();
            if (StringUtils.isBlank(value)) {
                return true;
            }
            Project project = item.requireTrait(HasProject.class).getProject();
            // match any segment of th fqpn

            return project.getFQPN().getSegments()
                    .stream()
                    .filter(segment -> !segment.equals(project.getWorkspaceId()))
                    .filter(segment -> !segment.equals(project.getConnectionId()))
                    .anyMatch(segment -> segment.toLowerCase().contains(value.toLowerCase()));
        });
        textField.addValueChangeListener(x -> handle.refresh());
        addDetachListener(e -> handle.remove());
    }

    public void setDecorated(boolean decorated) {
        if (decorated) {
            textField.setPrefixComponent(MaterialIcons.SEARCH.create());
            textField.setPlaceholder("Project");
        } else {
            textField.setPrefixComponent(null);
            textField.setPlaceholder(null);
        }
    }
}

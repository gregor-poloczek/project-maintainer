package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.AbstractComposable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasProject;
import org.apache.commons.lang3.StringUtils;

public class HasProjectFilterComponent<T extends AbstractComposable<?, T>> extends HorizontalLayout {

    public HasProjectFilterComponent(ComposableFilterSearch<T> composableFilterSearch) {
        TextField textField = new TextField();
        textField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        textField.setPlaceholder("Project");
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        this.add(textField);

        // participate in search
        var handle = composableFilterSearch.add(item -> {
            String value = textField.getValue();
            if (StringUtils.isBlank(value)) {
                return true;
            }
            Project project = item.requireTrait(HasProject.class).getProject();
            // match any segment of th fqpn
            return project.getFQPN().getSegments().stream()
                    .anyMatch(segment -> segment.toLowerCase().contains(value.toLowerCase()));
        });
        textField.addValueChangeListener(_ -> handle.refresh());
        addDetachListener(e -> handle.remove());
    }
}

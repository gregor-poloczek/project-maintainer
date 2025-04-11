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
import org.apache.commons.lang3.mutable.MutableObject;

public class HasProjectFilterComponent<T extends AbstractComposable<T>> extends HorizontalLayout {

    public HasProjectFilterComponent(ComposableFilterSearch<T> search) {
        MutableObject<String> query = new MutableObject<>("");
        TextField searchTextField = new TextField();
        searchTextField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchTextField.setPlaceholder("Project");
        searchTextField.setValueChangeMode(ValueChangeMode.EAGER);
        search.add(item -> {
            if (StringUtils.isBlank(query.getValue())) {
                return true;
            }
            Project project = item.requireTrait(HasProject.class).getProject();
            return project.getFQPN().toString().toLowerCase().contains(query.getValue().toLowerCase());
        });
        searchTextField.addValueChangeListener(e -> {
            query.setValue(e.getValue().toLowerCase());
            search.refresh();
        });
        this.add(searchTextField);
    }
}

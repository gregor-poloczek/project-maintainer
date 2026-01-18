package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasLabels;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HasLabelsFilterComponent extends HorizontalLayout {

    private final TextField labelsSearchFilter;

    public HasLabelsFilterComponent(ComposableFilterSearch<?, ?> search) {
        labelsSearchFilter = new TextField();
        labelsSearchFilter.setPlaceholder("Labels");
        labelsSearchFilter.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        labelsSearchFilter.setValueChangeMode(ValueChangeMode.EAGER);

        this.add(labelsSearchFilter);
        var handle = search.add(
                c -> StringUtils.isBlank(labelsSearchFilter.getValue()) || c.requireTrait(HasLabels.class)
                        .getLabels()
                        .stream()
                        .anyMatch(l -> l.getValue()
                                .toLowerCase()
                                .contains(labelsSearchFilter.getValue().toLowerCase())));
        labelsSearchFilter.addValueChangeListener(e -> handle.refresh());
        this.addDetachListener(e -> handle.refresh());
    }

    public String getValue() {
        return this.labelsSearchFilter.getValue();
    }
}

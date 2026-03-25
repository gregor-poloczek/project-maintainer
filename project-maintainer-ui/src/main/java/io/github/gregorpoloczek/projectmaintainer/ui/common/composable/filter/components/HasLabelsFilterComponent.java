package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasLabels;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HasLabelsFilterComponent extends HorizontalLayout {

    private final TextField labelsSearchFilter;

    public HasLabelsFilterComponent(ComposableFilterSearch<?, ?> search) {
        labelsSearchFilter = new TextField();
        labelsSearchFilter.setPlaceholder("Labels");
        labelsSearchFilter.setPrefixComponent(MaterialIcons.SEARCH.create());
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

package de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.components;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.filter.ComposableFilterSearch;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasWorkingCopy;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HasWorkingCopyFilterComponent extends HorizontalLayout {

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum FilterType {
        ALL("All"),
        ATTACHED("Attached"),
        DETACHED("Detached");
        String label;
    }

    public HasWorkingCopyFilterComponent(ComposableFilterSearch<?, ?> search) {
        ComboBox<FilterType> filterTypeCombobox = new ComboBox<>();
        filterTypeCombobox.setItems(FilterType.values());
        filterTypeCombobox.setValue(FilterType.ALL);
        filterTypeCombobox.setItemLabelGenerator(FilterType::getLabel);
        filterTypeCombobox.setWidth("130px");
        this.add(filterTypeCombobox);
        var handle = search.add(c -> {
            Optional<WorkingCopy> workingCopy = c.requireTrait(HasWorkingCopy.class).getWorkingCopy();
            return switch (filterTypeCombobox.getValue()) {
                case ATTACHED -> workingCopy.isPresent();
                case DETACHED -> workingCopy.isEmpty();
                case ALL -> true;
            };
        });
        filterTypeCombobox.addValueChangeListener(e -> handle.refresh());
        this.addDetachListener(e -> handle.refresh());
    }
}

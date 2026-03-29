package io.github.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import lombok.AccessLevel;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static io.github.gregorpoloczek.projectmaintainer.ui.common.VaadinUtils.with;

/**
 * Wrapper component that creates {@link PatchParameterArgument} based on the values produced by wrapped component via
 * {@link HasValue}. Additionally, enables the option of enabling and disabling optional parameters.
 *
 * @param <T> the value type of {@link PatchParameterArgument}
 * @param <C> the {@link Component} type (with must implement {@link HasValue} and {@link HasEnabled})
 * @param <E> the {@link HasValue.ValueChangeEvent} type
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatchParameterArgumentComponent<
        T,
        C extends Component & HasValue<E, T> & HasEnabled,
        E extends HasValue.ValueChangeEvent<T>>
        extends FlexLayout
        implements HasValue<HasValue.ValueChangeEvent<PatchParameterArgument<T>>, PatchParameterArgument<T>> {
    final PatchParameter patchParameter;
    final C component;

    private final Collection<ValueChangeListener<? super ValueChangeEvent<PatchParameterArgument<T>>>> valueChangeListeners = new ArrayList<>();

    boolean defined;

    @ToString
    public class PatchParameterArgumentImpl implements PatchParameterArgument<T> {
        @ToString.Include
        @Override
        public PatchParameter getParameter() {
            return PatchParameterArgumentComponent.this.patchParameter;
        }

        @ToString.Include
        @Override
        public Optional<T> getValue() {
            if (PatchParameterArgumentComponent.this.defined) {
                return Optional.of(PatchParameterArgumentComponent.this.component.getValue());
            }
            return Optional.empty();
        }
    }


    public PatchParameterArgumentComponent(PatchParameter patchParameter, C component) {
        this.patchParameter = patchParameter;
        this.component = component;
        this.setFlexDirection(FlexDirection.COLUMN);

        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(Alignment.CENTER);
        row.setPadding(false);


        if (!patchParameter.isRequired()) {
            this.defined = false;
            component.setEnabled(false);
            Checkbox definedCheckbox = new Checkbox();
            definedCheckbox.setTooltipText("Check if you want to define a value for this optional parameter.");
            definedCheckbox.addValueChangeListener(e -> {
                PatchParameterArgument<T> oldValue = this.getValue();
                this.defined = e.getValue();
                component.setEnabled(this.defined);

                var event = new AbstractField.ComponentValueChangeEvent<>(this, this, oldValue, true);
                valueChangeListeners
                        .forEach((ValueChangeListener<? super ValueChangeEvent<PatchParameterArgument<T>>> l) -> l.valueChanged(event));
            });
            row.add(definedCheckbox);
        } else {
            this.defined = true;
        }

        if (this.component instanceof HasTooltip hasTooltip) {
            patchParameter.getDescription().ifPresent(hasTooltip::setTooltipText);
        }

        this.component.addValueChangeListener(e -> {
            PatchParameterArgument<T> oldValue = this.getValue();

            var event = new AbstractField.ComponentValueChangeEvent<>(this, this, oldValue, true);
            valueChangeListeners
                    .forEach((ValueChangeListener<? super ValueChangeEvent<PatchParameterArgument<T>>> l) -> l.valueChanged(event));
        });

        row.add(component);

        String label = patchParameter.getName().orElse(patchParameter.getId());
        this.add(with(new NativeLabel(label), l -> l.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL)),
                row);
    }


    @Override
    public void setValue(PatchParameterArgument<T> tPatchParameterArgument) {

    }

    @Override
    public PatchParameterArgument<T> getValue() {
        return new PatchParameterArgumentImpl();
    }

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ValueChangeEvent<PatchParameterArgument<T>>> valueChangeListener) {
        return Registration.addAndRemove(this.valueChangeListeners, valueChangeListener);
    }

    @Override
    public void setReadOnly(boolean b) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isReadOnly() {
        throw new NotImplementedException();
    }

    @Override
    public void setRequiredIndicatorVisible(boolean b) {
        throw new NotImplementedException();

    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        throw new NotImplementedException();
    }
}

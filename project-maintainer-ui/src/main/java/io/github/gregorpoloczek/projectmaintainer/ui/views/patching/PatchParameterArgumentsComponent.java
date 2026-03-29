package io.github.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters.WellKnownPatchParameters;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import io.github.gregorpoloczek.projectmaintainer.ui.views.patching.components.FilesUploadParameterComponent;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Component grouping together components for editing patch parameters, and creating
 * {@link PatchParameterArgument} instances.
 */
public class PatchParameterArgumentsComponent extends FlexLayout {

    private Binder<Map<String, PatchParameterArgument<?>>> binder;

    public PatchParameterArgumentsComponent() {
    }

    void setParameters(List<PatchParameter> parameters) {
        this.removeAll();
        this.addClassNames(LumoUtility.Gap.SMALL);
        this.setFlexWrap(FlexLayout.FlexWrap.WRAP);

        binder = new Binder<>();
        this.binder.setBean(new LinkedHashMap<>());

        this.add(buildParameterComponent(WellKnownPatchParameters.BRANCH));

        for (PatchParameter patchParameter : parameters) {
            this.add(buildParameterComponent(patchParameter));
        }
    }

    private @NonNull Component buildParameterComponent(PatchParameter patchParameter) {
        Component c = switch (patchParameter.getType()) {
            case STRING -> new TextField();
            case INTEGER -> {
                IntegerField r = new IntegerField();
                r.setRequired(true);
                r.setValue(0);
                yield r;
            }
            case BOOLEAN -> {
                RadioButtonGroup<Boolean> radioGroup = new RadioButtonGroup<>();
                radioGroup.setItems(false, true);
                radioGroup.setValue(false);
                yield radioGroup;
            }
            case FILES -> new FilesUploadParameterComponent(patchParameter);
            default -> throw new IllegalStateException(patchParameter.getType().toString());
        };


        var result = new PatchParameterArgumentComponent(patchParameter, c);
        var b = binder.forField(result);
        b.bind(m -> {
            Map<String, PatchParameterArgument<?>> map = (Map<String, PatchParameterArgument<?>>) m;
            PatchParameterArgument<?> patchParameterArgument = map.get(patchParameter.getId());
            return patchParameterArgument;
        }, (m, v) -> {
            Map<String, PatchParameterArgument<?>> map = (Map<String, PatchParameterArgument<?>>) m;
            map.put(patchParameter.getId(), (PatchParameterArgument<?>) v);
        });

        if (c instanceof HasSize hasSize) {
            int width = patchParameter.isRequired() ? 542 : 500;
            hasSize.setWidth(width + "px");
        }

        return result;
    }


    public Map<String, PatchParameterArgument<?>> getValues() {
        return this.binder.getBean();
    }
}

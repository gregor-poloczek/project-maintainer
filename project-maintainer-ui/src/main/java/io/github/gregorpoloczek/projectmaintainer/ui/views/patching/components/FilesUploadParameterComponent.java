package io.github.gregorpoloczek.projectmaintainer.ui.views.patching.components;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterFile;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class FilesUploadParameterComponent extends VerticalLayout
        implements HasValue<HasValue.ValueChangeEvent<List<PatchParameterFile>>, List<PatchParameterFile>> {

    private List<PatchParameterFile> value = new ArrayList<>();
    private final Collection<ValueChangeListener<? super ValueChangeEvent<List<PatchParameterFile>>>> valueChangeListeners = new ArrayList<>();

    public FilesUploadParameterComponent(PatchParameter patchParameter) {
        this.setPadding(false);
        Upload upload = new Upload(UploadHandler.inMemory((a, b) -> {
            List<PatchParameterFile> newValue = Stream.concat(this.value.stream(), Stream.of(new PatchParameterFileImpl(a.fileName(), a.contentType(), b))).map(PatchParameterFile.class::cast).toList();
            this.setValue(newValue);
        }));
        upload.addClassNames(LumoUtility.Padding.SMALL);
        upload.setWidthFull();
        upload.addFileRemovedListener(e -> {
            List<PatchParameterFile> newValue = this.value.stream().filter(pF -> !pF.getFileName().equals(e.getFileName())).toList();
            this.setValue(newValue);
        });
        this.add(upload);
    }

    @Override
    public void setValue(List<PatchParameterFile> value) {
        if (value == null) {
            value = List.of();
        }
        List<PatchParameterFile> oldValue = this.value;
        this.value = value;
        // TODO [Patching] correct value for fromClient?
        var event = new AbstractField.ComponentValueChangeEvent<>(this, this, oldValue, true);
        valueChangeListeners
                .forEach((ValueChangeListener<? super ValueChangeEvent<List<PatchParameterFile>>> l) -> l.valueChanged(event));
    }

    @Override
    public List<PatchParameterFile> getValue() {
        return this.value;
    }

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ValueChangeEvent<List<PatchParameterFile>>> valueChangeListener) {
        return Registration.addAndRemove(this.valueChangeListeners, valueChangeListener);
    }

    @Override
    public void setReadOnly(boolean b) {
        throw new NotImplementedException("...");
    }

    @Override
    public boolean isReadOnly() {
        throw new NotImplementedException("...");
    }

    @Override
    public void setRequiredIndicatorVisible(boolean b) {
        throw new NotImplementedException("...");
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        throw new NotImplementedException("...");
    }
}

package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractGenericProjectConnectionFormComponent<T extends ProjectConnection> extends VerticalLayout
        implements ProjectConnectionFormComponent<T> {

    private final Binder<InternalForm> binder;
    private final InternalForm internalForm = new InternalForm();
    private final Collection<ValueChangeListener<? super ValueChangeEvent<ProjectConnectionForm<T>>>> listeners = new ArrayList<>();

    @Data
    public static class InternalForm {
        public boolean valid;
        String id = "";
        String type = "";
        Map<String, Object> data = new LinkedHashMap<>();
        ProjectConnectionForm.State state = ProjectConnectionForm.State.NEW;
    }

    @Override
    public void setValue(ProjectConnectionForm<T> value) {
        T connection = value.getConnection();
        this.internalForm.data.clear();
        if (connection != null) {
            this.internalForm.data.putAll(this.extractData(connection));
        } else {
            this.internalForm.data.putAll(getFieldDefinition().stream().collect(Collectors.toMap(FieldDefinition::getId, FieldDefinition::getDefaultValue)));
        }
        this.internalForm.state = value.getState();
        this.internalForm.id = value.getId();
        this.internalForm.type = value.getType();
        this.internalForm.valid = value.isValid();

        this.binder.readBean(this.internalForm);

        this.fireValueChangeEvent();
    }

    protected abstract Map<String, Object> extractData(T connection);

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ValueChangeEvent<ProjectConnectionForm<T>>> listener) {
        return Registration.addAndRemove(this.listeners, listener);
    }


    @Override
    public ProjectConnectionForm<T> getValue() {
        return ProjectConnectionForm.<T>builder()
                .type(this.internalForm.type)
                .id(this.internalForm.id)
                .connection(this.internalForm.valid ? this.createConnection(this.internalForm.id, this.internalForm.data) : null)
                .state(this.internalForm.state)
                .valid(this.internalForm.valid)
                .build();
    }

    @Builder
    @Value
    public static class FieldDefinition {
        String label;
        Type type;
        String id;
        Object defaultValue;

        public enum Type {
            STRING, SECRET_STRING
        }

    }

    public AbstractGenericProjectConnectionFormComponent(ImageResolverService imageResolverService, String type) {

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setMaxWidth("500px");
        List<FieldDefinition> definitions = this.getFieldDefinition();
        binder = new Binder<>(InternalForm.class);
        for (FieldDefinition field : definitions) {
            if (field.type == FieldDefinition.Type.STRING) {
                TextField textField = new TextField(field.getLabel());
                textField.setWidthFull();
                textField.addAttachListener(e -> this.internalForm.state = ProjectConnectionForm.State.DIRTY);
                textField.setValueChangeMode(ValueChangeMode.EAGER);
                form.add(textField);
                binder.forField(textField).asRequired()
                        .bind(i -> (String) i.data.get(field.getId()), (i, v) -> internalForm.data.put(field.getId(), v));
            } else if (field.type == FieldDefinition.Type.SECRET_STRING) {
                PasswordField passwordField = new PasswordField(field.getLabel());
                passwordField.setWidthFull();
                passwordField.setValueChangeMode(ValueChangeMode.EAGER);
                passwordField.addAttachListener(e -> this.internalForm.state = ProjectConnectionForm.State.DIRTY);
                form.add(passwordField);

                binder.forField(passwordField).asRequired()
                        .bind(i -> (String) i.data.get(field.getId()), (i, v) -> internalForm.data.put(field.getId(), v));
            } else {
                throw new IllegalStateException("Unsupported " + field.type);
            }
        }

        HorizontalLayout topRow = new HorizontalLayout();
        if (type != null) {
            topRow.add(new GitProviderIconComponent(imageResolverService, type));
        }
        topRow.add(new H4(getTitle()));
        topRow.setAlignItems(Alignment.CENTER);


        VerticalLayout right = createDescriptionComponent();

        HorizontalLayout bodyRow = new HorizontalLayout(form, right);
        bodyRow.setWidthFull();

        this.add(topRow, bodyRow);

        binder.setBean(this.internalForm);

        this.binder.addValueChangeListener(e -> {
            this.internalForm.valid = this.binder.isValid();
            fireValueChangeEvent();
        });
    }

    protected abstract @NonNull T createConnection(String id, Map<String, Object> data);

    protected VerticalLayout createDescriptionComponent() {
        return new VerticalLayout();
    }

    protected abstract List<FieldDefinition> getFieldDefinition();

    protected abstract @NonNull String getTitle();

    private void fireValueChangeEvent() {
        for (var l : this.listeners) {
            // TODO [Workspaces] 4. argument?
            l.valueChanged(new AbstractField.ComponentValueChangeEvent<>(this, this, getValue(), true));
        }
    }
}

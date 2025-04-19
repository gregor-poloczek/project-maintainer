package de.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VaadinUtils {

    public <T extends Component> void access(T component, Consumer<T> command) {
        component.getUI().filter(UI::isAttached).ifPresent(ui -> ui.access(() -> {
            if (!ui.isAttached()) {
                return;
            }
            command.accept(component);
        }));
    }

    public <T extends Component, P> void access(T component, P payload, BiConsumer<T, P> command) {
        component.getUI().filter(UI::isAttached).ifPresent(ui -> ui.access(() -> {
            if (!ui.isAttached()) {
                return;
            }
            command.accept(component, payload);
        }));
    }

}

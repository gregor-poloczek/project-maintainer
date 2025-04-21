package de.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
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

    public void show(Component... components) {
        Stream.of(components).forEach(c -> c.setVisible(true));
    }

    public void hide(Component... components) {
        Stream.of(components).forEach(c -> c.setVisible(false));
    }

    public static void toggleClassName(Component component, boolean value, String className) {
        if (value) {
            component.addClassName(className);
        } else {
            component.removeClassName(className);
        }
    }
}

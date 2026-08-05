package io.github.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.UI;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.component.shared.Tooltip;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VaadinUtils {

    public final static Consumer<ThemableLayout> NO_PADDING = l -> l.setPadding(false);
    public final static Consumer<FlexComponent> ALIGN_ITEMS_CENTER = l -> l.setAlignItems(FlexComponent.Alignment.CENTER);
    public final static Consumer<HasSize> WIDTH_FULL = HasSize::setWidthFull;
    public static final Consumer<? super Component> HIDDEN = c -> c.setVisible(false);

    @SafeVarargs
    public static <T extends Component> T with(T component, Consumer<? super T>... modifiers) {
        Stream.of(modifiers).forEach(m -> m.accept(component));
        return component;
    }

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

    public static Consumer<Component> withTooltip(String tooltip) {
        return c -> {
            if (c instanceof HasTooltip ht) {
                ht.setTooltipText(tooltip);
            } else {
                Tooltip t = Tooltip.forComponent(c);
                t.setText(tooltip);
            }
        };
    }

    public static class Notifications {
        private static final int DURATION_MS = 3000;

        public static void showError(String message) {
            Notification notification = new Notification(message);
            notification.addThemeVariants(NotificationVariant.ERROR);
            notification.setDuration(DURATION_MS);
            notification.open();
        }

        public static void showSuccess(String message) {
            Notification notification = new Notification(message);
            notification.addThemeVariants(NotificationVariant.SUCCESS);
            notification.setDuration(DURATION_MS);
            notification.open();
        }
    }

}

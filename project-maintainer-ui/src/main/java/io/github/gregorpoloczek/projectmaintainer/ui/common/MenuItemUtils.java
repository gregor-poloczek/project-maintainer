package io.github.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import lombok.experimental.UtilityClass;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;

@UtilityClass
public class MenuItemUtils {
    public static void createMenuItem(MenuBar menuBar, String tooltipText, MaterialIcons icon, String label, ComponentEventListener<ClickEvent<MenuItem>> callback) {
        Icon iconComponent = icon.create();
        iconComponent.getStyle().setMarginRight("var(--lumo-space-s)");
        MenuItem preview = menuBar.addItem(iconComponent, callback);
        preview.add(new Text(label));
        menuBar.setTooltipText(preview, tooltipText);
    }

}

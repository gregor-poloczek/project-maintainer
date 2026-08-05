package io.github.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IconFactory;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class IconText extends Composite<FlexLayout> {

    public IconText(Icon icon, String text) {
        this.getContent().addClassNames(LumoUtility.Gap.SMALL, LumoUtility.FlexDirection.ROW);
        this.getContent().add(icon, new Text(text));
    }

    public IconText(IconFactory iconFactory, String text) {
        this(iconFactory.create(), text);
    }
}

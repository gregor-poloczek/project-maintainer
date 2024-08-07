package de.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.html.Div;

public class DiffComponent extends Div {

    public DiffComponent(String diff) {
        Div diffContainer = new Div();
        diffContainer.addAttachListener(e -> {
            diffContainer.getElement().executeJs("""
                    renderDiff(this, $0)
                    """, diff);
        });
        add(diffContainer);
        setSizeFull();
    }

}

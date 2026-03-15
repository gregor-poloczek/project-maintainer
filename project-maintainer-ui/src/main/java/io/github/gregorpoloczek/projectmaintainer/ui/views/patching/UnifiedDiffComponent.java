package io.github.gregorpoloczek.projectmaintainer.ui.views.patching;

import com.vaadin.flow.component.html.Div;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.UnifiedDiff;

public class UnifiedDiffComponent extends Div {

    public UnifiedDiffComponent(UnifiedDiff unifiedDiff) {
        Div diffContainer = new Div();
        diffContainer.addAttachListener(e -> {
            diffContainer.getElement().executeJs("""
                    renderDiff(this, $0)
                    """, unifiedDiff.getValue());
        });
        add(diffContainer);
        setSizeFull();
    }

}

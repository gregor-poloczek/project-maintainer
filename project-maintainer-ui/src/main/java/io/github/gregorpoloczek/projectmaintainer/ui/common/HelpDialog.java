package io.github.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.dom.Style;
import org.intellij.lang.annotations.Language;
import org.vaadin.addons.gl0b3.materialicons.MaterialIcons;

@StyleSheet("./styles/common/HelpDialog.css")
public class HelpDialog extends Dialog {


    private final HelpFactoryProvider helpFactoryProvider;

    public void refresh() {
        HelpFactory helpFactory = this.helpFactoryProvider.getHelpFactory();
        if (helpFactory == null) {
            throw new IllegalStateException("No help factory defined.");
        }
        if (helpFactory instanceof MarkdownHelpFactory mhf) {
            this.content.setContent(mhf.createMarkdownHelp());
        } else {
            throw new IllegalStateException("Cannot handle " + helpFactory.getClass().getName());
        }
    }

    public interface HelpFactory {

    }

    public interface HelpFactoryProvider {
        HelpFactory getHelpFactory();
    }

    public interface MarkdownHelpFactory extends HelpFactory {
        @Language("markdown")
        String createMarkdownHelp();
    }

    private final Markdown content;


    public HelpDialog(HelpFactoryProvider helpFactoryProvider) {
        this.content = new Markdown();
        this.helpFactoryProvider = helpFactoryProvider;
        this.add(content);
        this.addClassName(HelpDialog.class.getSimpleName());
        this.setDraggable(true);
        this.setModality(ModalityMode.MODELESS);
        this.setResizable(true);
        this.setWidth("500px");
        Button closeButton = new Button(MaterialIcons.CLOSE.create(), e -> this.close());
        closeButton
                .getStyle().setPosition(Style.Position.ABSOLUTE)
                .setMargin("12px")
                .setTop("0").setRight("0");
        closeButton.setTooltipText("Close help");
        this.add(closeButton);
    }

    @Override
    public void open() {
        this.refresh();
        super.open();
    }
}

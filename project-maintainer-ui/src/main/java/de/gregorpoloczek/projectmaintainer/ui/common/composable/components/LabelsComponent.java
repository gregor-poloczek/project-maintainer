package de.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import de.gregorpoloczek.projectmaintainer.analysis.service.label.Label;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.Composable;
import de.gregorpoloczek.projectmaintainer.ui.common.composable.traits.HasLabels;
import io.micrometer.common.util.StringUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LabelsComponent extends FlexLayout {

    Map<Label, Html> labelComponents = new HashMap<>();
    private final Supplier<String> queryProvider;

    public static <C extends Composable<C>> Renderer<C> getRenderer(Supplier<String> queryProvider) {
        return new ComponentRenderer<>((C composable) -> new LabelsComponent(composable, queryProvider),
                ((component, composable) -> ((LabelsComponent) component).update(composable)));
    }


    public LabelsComponent(Composable<?> composable, Supplier<String> queryProvider) {
        this.queryProvider = queryProvider;
        this.setFlexDirection(FlexDirection.ROW);
        this.setFlexWrap(FlexWrap.WRAP);
        this.getStyle().set("gap", "4px");

        this.update(composable);
    }

    public LabelsComponent update(Composable<?> composable) {
        Collection<Label> labels = composable.requireTrait(HasLabels.class).getLabels();
        String query = queryProvider.get();

        for (Label label : labels) {
            Html html = labelComponents.get(label);
            if (html == null) {
                html = new Html("<span>%s</span>".formatted(label.getValue()));
                this.add(html);
                this.labelComponents.put(label, html);
            }
            if (StringUtils.isBlank(query)) {
                if (!html.isVisible()) {
                    html.setVisible(true);
                    html.setHtmlContent("<span>%s</span>".formatted(label.getValue()));
                }
            } else {
                if (label.getValue().toLowerCase().contains(query)) {
                    String adjusted =
                            label.getValue()
                                    .replaceAll("(\\Q" + query + "\\E)", "<b style=\"color: white;\">$1</b>");
                    html.setHtmlContent("<span style=\"color: gray;\">%s</span>".formatted(adjusted));
                } else {
                    html.setVisible(false);
                }
            }
        }
        return this;
    }
}

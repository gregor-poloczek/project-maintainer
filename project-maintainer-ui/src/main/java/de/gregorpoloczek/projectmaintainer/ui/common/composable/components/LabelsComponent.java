package de.gregorpoloczek.projectmaintainer.ui.common.composable.components;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
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
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.intellij.lang.annotations.Language;

@StyleSheet("styles/labels-component.css")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class LabelsComponent extends FlexLayout {

    Map<Label, Html> labelComponents = new HashMap<>();
    private final Supplier<String> queryProvider;
    Html inner;


    public static <C extends Composable<C>> Renderer<C> getRenderer(Supplier<String> queryProvider) {
        return new ComponentRenderer<>((C composable) -> {
            return new LabelsComponent(composable, queryProvider);
        },
                ((component, composable) -> ((LabelsComponent) component).update(composable)));
    }


    public LabelsComponent(Composable<?> composable, Supplier<String> queryProvider) {
        this.addClassName("labels-component");
        this.queryProvider = queryProvider;
        this.setFlexDirection(FlexDirection.ROW);
        this.setFlexWrap(FlexWrap.WRAP);
        this.getStyle().set("gap", "4px");
//        this.inner = new Html("<div></div>");
//        this.inner.addClassName("labels-component");
//        this.add(inner);
        this.setId(UUID.randomUUID().toString());

        System.out.println("constructor");

        this.update(composable);
    }

    int amount = 0;

    public LabelsComponent update(Composable<?> composable) {
        Collection<Label> labels = composable.requireTrait(HasLabels.class).getLabels();
        String query = queryProvider.get();
        System.out.println("update");

        if (this.isAttached()) {
            if (!StringUtils.isBlank(query)) {
                boolean hide = labels.stream().noneMatch(label -> label.getValue().matches(query));
                @Language("javascript")
                String code = """
                            console.warn("%s", document.getElementById('%s')?.parentNode);
                        """;
                String id = getId().orElseThrow();
                UI.getCurrent().getPage().executeJs(code.formatted(id, id));
            }
        }

//        if (amount != labels.size()) {
//            StringBuilder builder = new StringBuilder();
//            builder.append("<div>");
//            for (Label label : labels) {
//                builder.append("<span data-label=\"%s\">%s</span>".formatted(label.getValue(), label.getValue()));
//            }
//            builder.append("</div>");
//            this.inner.setHtmlContent(builder.toString());
//            this.amount = labels.size();
//        }
//
//        if (this.isAttached()) {
//            if (!StringUtils.isBlank(query)) {
//                for (Label label : labels) {
//                    boolean matches = label.getValue().toLowerCase().contains(query);
//                    @Language("javascript")
//                    String code = """
//                                const element = document.getElementById("%s");
//                                console.log(element);
//                                const label = element.querySelector('[data-label="%s"]');
//                                label.classList.toggle("not-matched", %s);
//                                label.classList.toggle("matched", %s);
//                                console.log(label);
//                            """;
//                    UI.getCurrent()
//                            .getPage()
//                            .executeJs(code.formatted(this.getId().orElseThrow(), label.getValue(), !matches, matches));
//                }
//            } else {
//                @Language("javascript")
//                String code = """
//                            const element = document.getElementById("%s");
//                            element.querySelectorAll('span').forEach(label => {
//                                label.classList.toggle("not-matched", false);
//                                label.classList.toggle("matched", false);
//                            });
//                        """;
//                UI.getCurrent()
//                        .getPage()
//                        .executeJs(code.formatted(this.getId().orElseThrow()));
//            }
//        }

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
                }
                if (html.hasClassName("matched")) {
                    html.setHtmlContent("<span>%s</span>".formatted(label.getValue()));
                    html.removeClassName("matched");
                }
            } else {
                if (label.getValue().toLowerCase().contains(query)) {
                    String adjusted =
                            label.getValue()
                                    .replaceAll("(\\Q" + query + "\\E)", "<span>$1</span>");
                    html.setHtmlContent("<span>%s</span>".formatted(adjusted));
                    html.addClassName("matched");
                } else {
                    html.removeClassName("matched");
                    html.setVisible(false);
                }
            }
        }
        return this;
    }
}

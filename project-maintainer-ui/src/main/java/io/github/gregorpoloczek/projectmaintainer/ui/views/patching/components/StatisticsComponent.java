package io.github.gregorpoloczek.projectmaintainer.ui.views.patching.components;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchOperationResultDetail;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StatisticsComponent extends HorizontalLayout {
    Map<PatchOperationResultDetail.Type, Set<FQPN>> typeToFQPNs = new HashMap<>();
    Map<FQPN, PatchOperationResultDetail.Type> fqpnToType = new HashMap<>();
    Map<PatchOperationResultDetail.Type, Span> labels = new HashMap<>();

    public StatisticsComponent() {
        for (PatchOperationResultDetail.Type type : PatchOperationResultDetail.Type.values()) {
            Span span = new Span();
            span.setVisible(false);
            labels.put(type, span);
            this.add(span);
        }
    }

    public synchronized void update(ProjectRelatable projectRelatable, PatchOperationResultDetail.Type type) {
        FQPN fqpn = projectRelatable.getFQPN();
        var currentType = fqpnToType.get(fqpn);
        if (currentType != null) {
            typeToFQPNs.get(currentType).remove(fqpn);
        }
        fqpnToType.put(fqpn, type);
        typeToFQPNs.computeIfAbsent(type, t -> new HashSet<>()).add(fqpn);

        update(type);
    }

    private void update(PatchOperationResultDetail.Type type) {
        int hits = Optional.ofNullable(typeToFQPNs.get(type)).map(Set::size).orElse(0);
        Span span = labels.get(type);
        span.setVisible(hits > 0);
        span.setText("%s: %d".formatted(
                StringUtils.capitalize(type.name().toLowerCase().replace("_", " ")),
                hits));
    }

    public synchronized void clear(ProjectRelatable projectRelatable) {
        FQPN fqpn = projectRelatable.getFQPN();
        var type = this.fqpnToType.get(fqpn);
        if (type != null) {
            this.typeToFQPNs.get(type).remove(fqpn);
        }
        this.fqpnToType.remove(fqpn);

        if (type != null) {
            update(type);
        }
    }

    public synchronized void clear() {
        Set.copyOf(this.fqpnToType.keySet()).forEach(this::clear);
    }


}

package de.gregorpoloczek.projectmaintainer.analysis.service.label;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class VersionedLabel extends Label {

    @Getter
    private final Label base;

    public VersionedLabel(Label label, String version) {
        super(VersionedLabel.join(label.getSegments(), version));
        this.base = label;
    }

    private static List<String> join(final List<String> segments, final String version) {
        final ArrayList list = new ArrayList(segments);
        list.add(version);
        return list;
    }

    public static VersionedLabel of(Label label, String version) {
        return new VersionedLabel(label, version);
    }

    public String getVersion() {
        return getSegments().getLast();
    }
}

package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;


import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Value
public class UnifiedDiffFile {
    String value;

    public enum Type {
        ADDED, MODIFIED, DELETED
    }

    public List<String> getLines() {
        return Stream.of(value.split("\n")).toList();
    }

    public Type getType() {
        if (this.value.contains("--- /dev/null")) {
            return Type.ADDED;
        } else if (this.value.contains("+++ /dev/null")) {
            return Type.DELETED;
        } else {
            return Type.MODIFIED;
        }
    }
}

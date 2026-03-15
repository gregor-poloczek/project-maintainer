package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import lombok.Getter;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
public class UnifiedDiff {
    @Getter
    List<UnifiedDiffFile> files;
    String value;

    public UnifiedDiff(String value) {
        this.value = value;
        new UnifiedDiffFile(value);
        this.files = Arrays.stream(this.value.split("(?=diff --git)")).map(UnifiedDiffFile::new).toList();
    }

    public boolean isEmpty() {
        return this.value.isEmpty();
    }


}

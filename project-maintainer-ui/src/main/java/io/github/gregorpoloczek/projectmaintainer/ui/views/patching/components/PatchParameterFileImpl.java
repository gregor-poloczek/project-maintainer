package io.github.gregorpoloczek.projectmaintainer.ui.views.patching.components;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatchParameterFileImpl implements PatchParameterFile {
    @Getter
    String fileName;
    @Getter
    String mimetype;
    byte[] bytes;

    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public long getSize() {
        return this.bytes.length;
    }

    @Override
    public String toString() {
        return "PatchParameterFile[fileName=\"%s\", mimetype=\"%s\", size=%d]".formatted(fileName, mimetype, bytes.length);
    }
}

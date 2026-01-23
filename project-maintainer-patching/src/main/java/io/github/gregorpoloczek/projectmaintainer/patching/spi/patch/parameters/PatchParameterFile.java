package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import java.io.InputStream;

public interface PatchParameterFile {
    String getFileName();

    String getMimetype();

    InputStream getInputStream();

    long getSize();
}

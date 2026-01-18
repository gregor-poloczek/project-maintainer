package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition;

public interface Patch {

    PatchMetaData getMetaData();

    void execute(PatchContext patchingContext);
}

package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common;

/**
 * Base interface for patches. Patches is reusable logic that constructs changes that are to be applied to the source
 * code of projects. The action of a {@link Patch} is either previewed or applied. Every modified file is detected and
 * treated as a change worthy of a commit.
 */
public interface Patch {

    /**
     * Returns meta information about the {@link Patch}.
     *
     * @return the meta-data of the {@link Patch}
     */
    PatchMetaData getMetaData();

    /**
     * Holds the logic of what is to be done as part of the patch.
     *
     * @param patchingContext the {@link PatchContext}
     */
    void execute(PatchContext patchingContext);
}

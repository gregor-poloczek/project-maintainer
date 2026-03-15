package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.operations;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.NonZeroStatusReturnedException;

import java.util.function.UnaryOperator;

/**
 * A set of available operations that can be performed during patch. A {@link io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.Patch}
 * can perform any kind of operation in principle, and is not restricted by the operations offered here. These methods
 * are merely intended to be used as useful helper functions.
 */
public interface PatchOperations {
    /**
     * Creates a file with the passed content (using charset {@link java.nio.charset.StandardCharsets#UTF_8})
     *
     * @param location the file's location
     * @param content  the file's content
     */
    void create(ProjectFileLocation location, String content);

    /**
     * Updates a file with the passed content (using charset {@link java.nio.charset.StandardCharsets#UTF_8})
     *
     * @param location the file's location
     * @param content  the file's content
     */
    void update(ProjectFileLocation location, String content);

    /**
     * Updates a file denoted by the {@link ProjectFileLocation} using a transformation function.
     * Will read the file, and write the transformation output back to the file (using charset
     * {@link java.nio.charset.StandardCharsets#UTF_8}).
     *
     * @param location       the file's location
     * @param transformation the transformation function
     */
    void update(ProjectFileLocation location, UnaryOperator<String> transformation);

    /**
     * Deletes a file denoted by the {@link ProjectFileLocation}.
     *
     * @param location the file's location
     */
    void delete(ProjectFileLocation location);

    /**
     * Runs a child process within the working copies root directory. IO will be inherited by default. Will throw a
     * {@link NonZeroStatusReturnedException} in case of a non-zero status code.
     *
     * @param processBuilderModifier the callback to modify the {@link ProcessBuilder}
     * @throws NonZeroStatusReturnedException when a non-zero status returns from the child process
     */
    void runProcess(UnaryOperator<ProcessBuilder> processBuilderModifier);

}

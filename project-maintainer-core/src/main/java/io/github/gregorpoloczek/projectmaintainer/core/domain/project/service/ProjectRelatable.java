package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service;

/**
 * Something that relates to exactly one project, identified by its {@link FQPN}.
 * <p>
 * APIs that related to a project accept this interface instead of a concrete type, so any of these objects
 * can be passed directly, without extracting the underlying project and its {@link FQPN} first.
 * </p>
 */
public interface ProjectRelatable {

    /**
     * Returns the fully qualified project name of the project this object relates to.
     *
     * @return the {@link FQPN} of the related project, never {@code null}
     */
    FQPN getFQPN();
}

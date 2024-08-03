package de.gregorpoloczek.projectmaintainer.ui.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

@FunctionalInterface
public interface HasProject {

    Project getProject();
}

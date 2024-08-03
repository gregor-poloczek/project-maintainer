package de.gregorpoloczek.projectmaintainer.ui.common.composable;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

@FunctionalInterface
public interface HasProject {

    Project getProject();
}

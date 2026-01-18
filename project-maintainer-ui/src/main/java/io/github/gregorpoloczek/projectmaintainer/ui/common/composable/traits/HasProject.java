package io.github.gregorpoloczek.projectmaintainer.ui.common.composable.traits;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

@FunctionalInterface
public interface HasProject {

    Project getProject();
}

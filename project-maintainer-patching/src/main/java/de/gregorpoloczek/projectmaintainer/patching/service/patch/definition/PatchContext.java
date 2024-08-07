package de.gregorpoloczek.projectmaintainer.patching.service.patch.definition;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common.ProjectFiles;
import java.util.function.Function;

public interface PatchContext extends ProjectRelatable {

    Project getProject();

    void create(ProjectFileLocation location, String content);

    void update(ProjectFileLocation location, String content);

    void update(ProjectFileLocation location, Function<String, String> transformation);

    void delete(ProjectFileLocation location);

    ProjectFiles files();

    void pullRequestTitle(String formatted);
}

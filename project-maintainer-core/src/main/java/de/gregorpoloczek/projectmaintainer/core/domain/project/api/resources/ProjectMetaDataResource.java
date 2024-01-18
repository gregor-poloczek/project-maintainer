package de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.Label;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public record ProjectMetaDataResource(FQPN fqpn, String name, String owner, URI uri,
                                      List<Label> labels) {

  public static ProjectMetaDataResource of(Project project) {
    final ProjectMetaData metaData = project.getMetaData();

    return new ProjectMetaDataResource(project.getFQPN(), metaData.getName(),
        metaData.getOwner(),
        metaData.getURI(),
        new ArrayList<>(project.getLabels())
    );
  }
}

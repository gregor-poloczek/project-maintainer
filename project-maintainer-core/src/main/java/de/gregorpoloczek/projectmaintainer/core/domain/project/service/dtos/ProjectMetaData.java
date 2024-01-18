package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;

public interface ProjectMetaData {

  static ProjectMetaDataBuilder builder() {
    // TODO lombok austesten
    return new ProjectMetaDataBuilder();
  }

  String getOwner();

  String getName();

  URI getURI();

  FQPN getFQPN();

}

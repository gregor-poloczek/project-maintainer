package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectResource {

    String key;
}

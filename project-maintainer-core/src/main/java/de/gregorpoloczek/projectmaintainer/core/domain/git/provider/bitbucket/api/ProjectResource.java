package de.gregorpoloczek.projectmaintainer.core.domain.git.provider.bitbucket.api;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectResource {

    String key;
}

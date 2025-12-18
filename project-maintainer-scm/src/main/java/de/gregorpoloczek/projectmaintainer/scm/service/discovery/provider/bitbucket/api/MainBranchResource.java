package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MainBranchResource {
    String type;
    String name;
}

package de.gregorpoloczek.projectmaintainer.git.service.discovery.provider.bitbucket.api;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepositoryLinkResource {

    String name;
    String href;
}

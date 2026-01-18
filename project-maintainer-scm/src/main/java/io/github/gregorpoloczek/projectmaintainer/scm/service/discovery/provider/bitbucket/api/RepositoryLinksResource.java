package io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api;

import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepositoryLinksResource {

    List<RepositoryLinkResource> clone;
    RepositoryLinkResource html;
    RepositoryLinkResource avatar;
    RepositoryLinkResource self;
}

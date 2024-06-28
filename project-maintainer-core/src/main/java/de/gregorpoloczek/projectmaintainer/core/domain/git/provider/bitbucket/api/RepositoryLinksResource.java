package de.gregorpoloczek.projectmaintainer.core.domain.git.provider.bitbucket.api;

import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepositoryLinksResource {

    List<RepositoryLinkResource> clone;
}

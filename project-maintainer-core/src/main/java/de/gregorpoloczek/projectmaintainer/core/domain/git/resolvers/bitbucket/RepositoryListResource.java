package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.bitbucket;

import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepositoryListResource {

    List<RepositoryResource> values;
}

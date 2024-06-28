package de.gregorpoloczek.projectmaintainer.git.provider.bitbucket.api;

import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepositoryListResource {

    int size;
    int page;
    int pagelen;
    String next;
    List<RepositoryResource> values;
}

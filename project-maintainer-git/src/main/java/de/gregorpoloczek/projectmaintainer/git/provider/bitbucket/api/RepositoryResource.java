package de.gregorpoloczek.projectmaintainer.git.provider.bitbucket.api;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepositoryResource {

    String name;
    String description;
    ProjectResource project;
    RepositoryLinksResource links;
}

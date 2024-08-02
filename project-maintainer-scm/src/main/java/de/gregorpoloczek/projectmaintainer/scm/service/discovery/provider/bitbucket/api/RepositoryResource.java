package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepositoryResource {

    ProjectResource project;
    String name;
    String description;
    String website;
    RepositoryLinksResource links;
}

package de.gregorpoloczek.projectmaintainer.scm;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties("project-maintainer.projects.discovery.aws-codecommit")
@Component
public class ProjectsDiscoveryAWSCodeCommitProperties {

    List<AWSCodeCommitLocation> locations = new ArrayList<>();
}

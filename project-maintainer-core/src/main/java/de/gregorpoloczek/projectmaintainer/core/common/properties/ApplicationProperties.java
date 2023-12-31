package de.gregorpoloczek.projectmaintainer.core.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("project-maintainer")
@Component
@Getter
@Setter
public class ApplicationProperties {

  private Projects projects;
}

package io.github.gregorpoloczek.projectmaintainer.core.common;


import io.github.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// TODO [Setup] is this obsolete?
@EnableAsync
@EnableConfigurationProperties(ApplicationProperties.class)
public class ApplicationConfiguration implements AsyncConfigurer, WebMvcConfigurer {

    // TODO [Setup] is this obsolete?
    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        registry.addMapping("/**");
    }
}

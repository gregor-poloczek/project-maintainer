package de.gregorpoloczek.projectmaintainer.core.common;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public ConversionService conversionService() {
    return new DefaultConversionService();
  }
}

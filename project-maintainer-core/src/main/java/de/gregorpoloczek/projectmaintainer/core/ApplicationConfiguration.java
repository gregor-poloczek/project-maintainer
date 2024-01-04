package de.gregorpoloczek.projectmaintainer.core;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class ApplicationConfiguration {

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    SimpleModule module = new SimpleModule();
    // TODO @JsonComponent
    module.addSerializer(FQPN.class, new FQPNSerializer());
    module.addDeserializer(FQPN.class, new FQPNDeserializer());

    return new ObjectMapper()
        .registerModule(module);
  }

  // TODO write jackson converter for FQPN (is this possible via ConversionService?)

  @Bean
  public ConversionService conversionService() {
    return new DefaultConversionService();
  }
}

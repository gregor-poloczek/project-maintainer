package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.gregorpoloczek.projectmaintainer.core.domain.encryption.service.EncryptionService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.encryption.service.SecretString;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkspaceConfiguration {

    @Bean
    public ObjectMapper workspaceFileObjectMapper(EncryptionService encryptionService) {
        ObjectMapper result = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addSerializer(FQPN.class, new FQPNSerializer());
        module.addDeserializer(FQPN.class, new FQPNDeserializer());
        module.addSerializer(SecretString.class, encryptionService.new SecretStringSerializer());
        module.addDeserializer(SecretString.class, encryptionService.new SecretStringDeserializer());

        result.registerModule(module);
        result.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        return result;
    }
}

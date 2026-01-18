package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.gregorpoloczek.projectmaintainer.core.domain.encryption.service.EncryptionService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.encryption.service.SecretString;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.stream.Stream;

@Configuration
public class WorkspaceConfiguration {
    public static class FQPNSerializer extends JsonSerializer<FQPN> {

        @Override
        public void serialize(FQPN value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getValue());
        }
    }

    public static class FQPNDeserializer extends JsonDeserializer<FQPN> {

        @Override
        public FQPN deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new FQPN(Stream.of(p.getValueAsString().split(FQPN.SEPARATOR)).toList());
        }
    }

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

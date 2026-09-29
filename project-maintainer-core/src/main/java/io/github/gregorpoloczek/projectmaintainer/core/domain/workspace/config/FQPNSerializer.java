package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;

import java.io.IOException;

public class FQPNSerializer extends JsonSerializer<FQPN> {

    @Override
    public void serialize(FQPN value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getValue());
    }
}

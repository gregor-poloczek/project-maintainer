package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;

import java.io.IOException;
import java.util.stream.Stream;

public class FQPNDeserializer extends JsonDeserializer<FQPN> {

    @Override
    public FQPN deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return new FQPN(Stream.of(p.getValueAsString().split(FQPN.SEPARATOR)).toList());
    }
}

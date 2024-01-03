package de.gregorpoloczek.projectmaintainer.core;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.io.IOException;

public class FQPNSerializer extends JsonSerializer<FQPN> {

  @Override
  public void serialize(final FQPN fqpn, final JsonGenerator jsonGenerator,
      final SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeString(fqpn.getValue());
  }

}

package de.gregorpoloczek.projectmaintainer.core;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.io.IOException;

public class FQPNDeserializer extends JsonDeserializer<FQPN> {

  @Override
  public FQPN deserialize(final JsonParser jsonParser,
      final DeserializationContext deserializationContext)
      throws IOException, JacksonException {
    return FQPN.of(jsonParser.getValueAsString());
  }
}

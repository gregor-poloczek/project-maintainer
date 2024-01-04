package de.gregorpoloczek.projectmaintainer.core.domain.project.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class FQPNDeserializer extends JsonDeserializer<FQPN> {

  @Override
  public FQPN deserialize(final JsonParser jsonParser,
      final DeserializationContext deserializationContext)
      throws IOException {
    return FQPN.of(jsonParser.getValueAsString());
  }
}

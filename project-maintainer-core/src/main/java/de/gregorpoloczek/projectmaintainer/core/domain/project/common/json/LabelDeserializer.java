package de.gregorpoloczek.projectmaintainer.core.domain.project.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.Label;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class LabelDeserializer extends JsonDeserializer<Label> {

    @Override
    public Label deserialize(final JsonParser jsonParser,
            final DeserializationContext deserializationContext)
            throws IOException {
        return Label.of(jsonParser.getValueAsString());
    }
}

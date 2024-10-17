package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.pattern.attr.NameAttribute;

import java.io.IOException;

public class NameAttrSerializer extends JsonSerializer<NameAttribute> {
    @Override
    public void serialize(NameAttribute nameAttribute, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField("name", nameAttribute.getValue());
    }
}

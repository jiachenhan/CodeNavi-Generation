package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.pattern.attr.MoTypeAttribute;

import java.io.IOException;

public class MoTypeAttrSerializer extends JsonSerializer<MoTypeAttribute> {
    @Override
    public void serialize(MoTypeAttribute moTypeAttribute, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("moType", moTypeAttribute.getValue().toString());
        jsonGenerator.writeEndObject();
    }
}

package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.pattern.attr.LocationSubTypeAttribute;

import java.io.IOException;

public class LocationSubTypeAtrSerializer extends JsonSerializer<LocationSubTypeAttribute> {
    @Override
    public void serialize(LocationSubTypeAttribute locationSubTypeAttribute, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField("locationSubType",
                locationSubTypeAttribute.getValue() == null ? "null" : locationSubTypeAttribute.getValue().toString());
    }
}

package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.pattern.attr.ExprTypeAttribute;

import java.io.IOException;

public class ExprTypeAttrSerializer extends JsonSerializer<ExprTypeAttribute> {
    @Override
    public void serialize(ExprTypeAttribute exprTypeAttribute, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField("exprType", exprTypeAttribute.getValue());
    }
}

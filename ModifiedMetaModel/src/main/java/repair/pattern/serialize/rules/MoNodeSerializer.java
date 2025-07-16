package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.ast.MoNode;

import java.io.IOException;

public class MoNodeSerializer extends JsonSerializer<MoNode> {
    @Override
    public void serialize(MoNode moNode, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", moNode.getId());
        jsonGenerator.writeStringField("type", moNode.getClass().getSimpleName());
        jsonGenerator.writeStringField("value", moNode.toString());
        jsonGenerator.writeNumberField("startLine", moNode.getStartLine());
        jsonGenerator.writeNumberField("endLine", moNode.getEndLine());
        jsonGenerator.writeEndObject();
    }
}

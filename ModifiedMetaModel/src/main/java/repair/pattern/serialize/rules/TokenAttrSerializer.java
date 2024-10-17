package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.pattern.attr.TokenAttribute;

import java.io.IOException;
import java.util.stream.Collectors;

public class TokenAttrSerializer extends JsonSerializer<TokenAttribute> {
    @Override
    public void serialize(TokenAttribute tokenAttribute, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String result = String.join(", ", tokenAttribute.getValue()); // 逗号分隔符
        jsonGenerator.writeStringField("token", result);
    }
}

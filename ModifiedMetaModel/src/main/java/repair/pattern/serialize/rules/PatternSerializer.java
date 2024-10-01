package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.ast.MoNode;
import repair.ast.code.statement.MoStatement;
import repair.pattern.Pattern;
import repair.pattern.attr.Attribute;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PatternSerializer extends JsonSerializer<Pattern> {
    @Override
    public void serialize(Pattern pattern, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        List<MoStatement> statements = pattern.getNodeToConsidered().keySet().stream()
                .filter(node -> node instanceof MoStatement)
                .map(node -> (MoStatement) node)
                .toList();

        generateStmts(statements, jsonGenerator); // part 1: stmts
        generateNodes(pattern.getNodeToConsidered().keySet(), jsonGenerator, serializerProvider); // part 2: nodes
        generateAttrs(pattern.getNodeToAttributes(), jsonGenerator, serializerProvider); // part 3: attrs
        jsonGenerator.writeEndObject();
    }


    private Optional<MoStatement> findBelongStmts(MoNode node) {
        if(node instanceof MoStatement) {
            return Optional.of((MoStatement) node);
        }
        MoNode parent = node.getParent();
        while (parent != null) {
            if(parent instanceof MoStatement) {
                return Optional.of((MoStatement) parent);
            }
            parent = parent.getParent();
        }
        return Optional.empty();
    }

    private void generateStmts(List<MoStatement> statements, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("Stmts");
        jsonGenerator.writeStartArray();
        for (MoStatement statement : statements) {
            String stmtStr = statement.toString();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("id", statement.getId());
            jsonGenerator.writeStringField("stmt", stmtStr);
            jsonGenerator.writeNumberField("startLine", statement.getStartLine());
            jsonGenerator.writeNumberField("endLine", statement.getEndLine());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private void generateNodes(Set<MoNode> nodes, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeFieldName("Nodes");
        // group nodes by their parent statement
        Map<MoStatement, List<MoNode>> statementSubNodes = nodes.stream()
                .map(node -> Map.entry(findBelongStmts(node), node))
                .filter(entry -> entry.getKey().isPresent())
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().get(),
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        jsonGenerator.writeStartArray();
        for (Map.Entry<MoStatement, List<MoNode>> entry : statementSubNodes.entrySet()) {
            MoStatement stmt = entry.getKey();
            List<MoNode> subNodes = entry.getValue();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("stmtId", stmt.getId());
            jsonGenerator.writeFieldName("subNodes");
            jsonGenerator.writeStartArray();
            JsonSerializer<Object> patternNodeSerializer = serializerProvider.findValueSerializer(MoNode.class);
            for (MoNode subNode : subNodes) {
                patternNodeSerializer.serialize(subNode, jsonGenerator, serializerProvider);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private void generateAttrs(Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes,
                               JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeFieldName("Attrs");
        jsonGenerator.writeStartArray();
        for (Map.Entry<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> entry : nodeToAttributes.entrySet()) {
            MoNode node = entry.getKey();
            Map<Class<? extends Attribute<?>>, Attribute<?>> attrs = entry.getValue();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("nodeId", node.getId());
            jsonGenerator.writeFieldName("attrs");
            jsonGenerator.writeStartArray();
            for (Map.Entry<Class<? extends Attribute<?>>, Attribute<?>> attrEntry : attrs.entrySet()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("attrType", attrEntry.getKey().getName());
                jsonGenerator.writeFieldName("attr");
                JsonSerializer<Object> attrSerializer = serializerProvider.findValueSerializer(attrEntry.getKey());
                attrSerializer.serialize(attrEntry.getValue(), jsonGenerator, serializerProvider);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}

package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import repair.ast.MoNode;
import repair.ast.code.statement.MoStatement;
import repair.pattern.Pattern;

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

        generateStmts(statements, jsonGenerator); // module 1: stmts
        generateNodes(pattern.getNodeToConsidered().keySet(), jsonGenerator, serializerProvider); // module 2: nodes

        // todo: pattern
        JsonSerializer<Object> patternNodeSerializer = serializerProvider.findValueSerializer(MoNode.class);
        patternNodeSerializer.serialize(pattern.getPatternBefore0(), jsonGenerator, serializerProvider);
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
}

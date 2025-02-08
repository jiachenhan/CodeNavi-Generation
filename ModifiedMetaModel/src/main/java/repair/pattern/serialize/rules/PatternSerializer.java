package repair.pattern.serialize.rules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import repair.FileUtils;
import repair.ast.MoNode;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.statement.MoStatement;
import repair.pattern.InsertNode;
import repair.pattern.MoveNode;
import repair.pattern.NotLogicManager;
import repair.pattern.Pattern;
import repair.pattern.attr.Attribute;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PatternSerializer extends JsonSerializer<Pattern> {
    @Override
    public void serialize(Pattern pattern, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        generateBeforeCode(pattern.getPatternBefore0(), jsonGenerator); // part 1: before code
        generateDiff(pattern.getPatternBefore0(), pattern.getPatternAfter0(), jsonGenerator); // part 2: diff

        jsonGenerator.writeFieldName("Before0Tree");
        generateNodeTree(pattern.getPatternBefore0(), jsonGenerator); // part 3: before0 tree

        if (pattern.getNotLogicManager().isPresent()) {
            NotLogicManager notLogicManager = pattern.getNotLogicManager().get();
            generateInsertNodes(notLogicManager, jsonGenerator);
            generateMoveNodes(notLogicManager, jsonGenerator);
        }

        generateAttrs(pattern.getNodeToAttributes(), jsonGenerator, serializerProvider); // part 5: attrs
        jsonGenerator.writeEndObject();
    }

    private void generateBeforeCode(MoNode beforeCode, JsonGenerator jsonGenerator) throws IOException {
        Path fileName = beforeCode.getFileName();
        jsonGenerator.writeStringField("FileName", fileName.toString());
        List<String> codes = Files.readAllLines(fileName, Charset.forName(FileUtils.detectCharset(fileName)));

        jsonGenerator.writeFieldName("BeforeCode");
        jsonGenerator.writeStartArray();
        for (int i = beforeCode.getStartLine() - 1; i < beforeCode.getEndLine(); i++) {
            jsonGenerator.writeString((i + 1) + ": " + codes.get(i));
        }
        jsonGenerator.writeEndArray();
    }

    private void generateDiff(MoNode beforeCode, MoNode afterCode, JsonGenerator jsonGenerator) throws IOException {
        Path beforeFileName = beforeCode.getFileName();
        Path afterFileName = afterCode.getFileName();
        List<String> original = Files.readAllLines(beforeFileName, Charset.forName(FileUtils.detectCharset(beforeFileName)));
        List<String> revised = Files.readAllLines(afterFileName, Charset.forName(FileUtils.detectCharset(afterFileName)));
        jsonGenerator.writeFieldName("Diff");

        Patch<String> patch = DiffUtils.diff(original, revised);
        jsonGenerator.writeStartArray();
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int lineStart = delta.getSource().getPosition() + 1;
            if(lineStart < beforeCode.getStartLine() || lineStart > beforeCode.getEndLine()) {
                continue;
            }
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("Type", delta.getType().toString());
            jsonGenerator.writeNumberField("LineStart", lineStart);
            jsonGenerator.writeFieldName("Original");
            jsonGenerator.writeStartArray();
            for (String line : delta.getSource().getLines()) {
                jsonGenerator.writeString(line);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeFieldName("Revised");
            jsonGenerator.writeStartArray();
            for (String line : delta.getTarget().getLines()) {
                jsonGenerator.writeString(line);
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private void generateNodeTree(MoNode node, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", node.getId());
        jsonGenerator.writeStringField("type", node.getClass().getSimpleName());
        jsonGenerator.writeBooleanField("isExpr", node instanceof MoExpression);
        jsonGenerator.writeStringField("value", node.toString());
        jsonGenerator.writeBooleanField("leaf", node.isLeaf());
        jsonGenerator.writeNumberField("startLine", node.getStartLine());
        jsonGenerator.writeNumberField("endLine", node.getEndLine());
        if(!node.isLeaf()) {
            jsonGenerator.writeFieldName("children");
            jsonGenerator.writeStartArray();
            for (MoNode child : node.getChildren()) {
                generateNodeTree(child, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndObject();
    }

    private void generateInsertNodes(NotLogicManager notLogicManager, JsonGenerator jsonGenerator) throws IOException {
        List<InsertNode> insertNodes = notLogicManager.getInsertNodes();
        jsonGenerator.writeFieldName("insertNodes");
        jsonGenerator.writeStartArray();
        for (InsertNode insertNode : insertNodes) {
            generateNodeTree(insertNode.insertNode(), jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void generateMoveNodes(NotLogicManager notLogicManager, JsonGenerator jsonGenerator) throws IOException {
        List<MoveNode> moveNodes = notLogicManager.getMoveNodes();
        jsonGenerator.writeFieldName("moveParentNodes");
        jsonGenerator.writeStartArray();
        for (MoveNode moveNode : moveNodes) {
            generateNodeTree(moveNode.moveParent(), jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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
        jsonGenerator.writeStartObject();
        for (Map.Entry<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> entry : nodeToAttributes.entrySet()) {
            MoNode node = entry.getKey();
            Map<Class<? extends Attribute<?>>, Attribute<?>> attrs = entry.getValue();
            jsonGenerator.writeFieldName(String.valueOf(node.getId()));
            jsonGenerator.writeStartObject();
            for (Map.Entry<Class<? extends Attribute<?>>, Attribute<?>> attrEntry : attrs.entrySet()) {
                JsonSerializer<Object> attrSerializer = serializerProvider.findValueSerializer(attrEntry.getKey());
                attrSerializer.serialize(attrEntry.getValue(), jsonGenerator, serializerProvider);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
    }
}

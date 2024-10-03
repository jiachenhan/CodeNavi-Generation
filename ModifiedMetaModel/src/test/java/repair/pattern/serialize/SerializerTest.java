package repair.pattern.serialize;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.modify.diff.DiffComparator;
import repair.pattern.Pattern;
import repair.pattern.abstraction.Abstractor;
import repair.pattern.abstraction.TermFrequencyAbstractor;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class SerializerTest {
    private final Path datasetPath = Paths.get("E:/dataset/c3/drjava1");
    private final Path serializePath = Paths.get("01pattern");
    private final Path jsonSerializePath = Paths.get("02pattern-info");

    @Test
    public void serializeTest() {
        Path groupPath = datasetPath.resolve("14");
        Path patternCasePath = null;
        List<Path> otherCasesPath = null;

        // 处理第三级：case
        try (Stream<Path> caseStream = Files.list(groupPath)) {
            List<Path> caseList = caseStream.toList();
            if (caseList.size() < 2) {
                System.out.println("Case less than 2, skip");
            }
            patternCasePath = caseList.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert patternCasePath != null;
        Pattern pattern = generatePattern(patternCasePath);

        Serializer.serializeToDisk(pattern, serializePath.resolve("ori").resolve("pattern.ser"));

        Abstractor abstractor = new TermFrequencyAbstractor();
        abstractor.doAbstraction(pattern);

        Serializer.serializeToDisk(pattern, serializePath.resolve("abs").resolve("pattern_abstracted.ser"));
    }

    @Test
    public void jsonSerializeTest() {
        Path groupPath = datasetPath.resolve("15");
        Path patternCasePath = null;
        List<Path> otherCasesPath = null;

        // 处理第三级：case
        try (Stream<Path> caseStream = Files.list(groupPath)) {
            List<Path> caseList = caseStream.toList();
            if (caseList.size() < 2) {
                System.out.println("Case less than 2, skip");
            }
            patternCasePath = caseList.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert patternCasePath != null;
        Pattern pattern = generatePattern(patternCasePath);

        String json = JsonSerializer.serializeToJson(pattern);
        try(FileWriter file = new FileWriter(jsonSerializePath.resolve("pattern.json").toFile())) {
            file.write(Objects.requireNonNull(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void deserializeTest() {
        Pattern pattern = Serializer.deserializeFromDisk(serializePath.resolve("pattern.ser")).orElse(null);
        assertNotNull(pattern);

        Pattern patternAbstracted = Serializer.deserializeFromDisk(serializePath.resolve("pattern_abstracted.ser")).orElse(null);
        assertNotNull(patternAbstracted);
    }

    private Pattern generatePattern(Path patternCase) {
        System.out.println("Processing case: " + patternCase.getFileName());
        Path patternBeforePath = patternCase.resolve("before.java");
        Path patternAfterPath = patternCase.resolve("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(patternBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(patternAfterPath);

        Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
        Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(patternBeforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(patternAfterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE,
                beforeParser.getIdentifierManager(), afterParser.getIdentifierManager());
    }

}
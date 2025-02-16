package repair.pattern.abstraction;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.dsl.kirin.LoadPatternGenerateTest;
import repair.dsl.kirin.QueryGenerator;
import repair.dsl.kirin.query.Query;
import repair.pattern.Pattern;
import repair.pattern.serialize.Serializer;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class LLMAbstractorTest {
    private static final Logger logger = LoggerFactory.getLogger(LLMAbstractorTest.class);

    @Test
    public void test() {
        Path beforePatternPath = Path.of("D:/workspace/CodeNavi-Generation/01pattern");
        Path abstractInfoPath = Path.of("D:/workspace/CodeNavi-Generation/02pattern-info");

        Optional<Pattern> patternOpt = Serializer.deserializeFromDisk(beforePatternPath);
        if (patternOpt.isEmpty()) {
            logger.error("Failed to read pattern from: {}", beforePatternPath);
            return;
        }

        Pattern pattern = patternOpt.get();
        Abstractor abstractor = new LLMAbstractor(abstractInfoPath);
        abstractor.doAbstraction(pattern);

        Query query = QueryGenerator.getInstance().generate(pattern);
        System.out.println(query.prettyPrint());
    }
}
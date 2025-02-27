package repair.dsl.kirin;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.dsl.kirin.query.Query;
import repair.pattern.Pattern;
import repair.pattern.serialize.Serializer;

import java.nio.file.Path;
import java.util.Optional;

public class LoadPatternGenerateTest {
    private static final Logger logger = LoggerFactory.getLogger(LoadPatternGenerateTest.class);

    @Test
    public void test() {
        Path path = Path.of("D:/workspace/CodeNavi-Generation/01pattern/pmd_hiera_60/abs/9/0.ser");

        Optional<Pattern> patternOpt = Serializer.deserializeFromDisk(path);
        if (patternOpt.isEmpty()) {
            logger.error("Failed to read pattern from: {}", path);
            return;
        }

        Pattern pattern = patternOpt.get();

        Query query = QueryGenerator.getInstance().generate(pattern);
        System.out.println(query.prettyPrint());
    }

}

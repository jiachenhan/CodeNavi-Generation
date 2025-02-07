package repair.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.pattern.Pattern;
import repair.pattern.abstraction.Abstractor;
import repair.pattern.abstraction.LLMAbstractor;
import repair.pattern.serialize.Serializer;

import java.nio.file.Path;
import java.util.Optional;


public class Abstract {
    private final static Logger logger = LoggerFactory.getLogger(Abstract.class);

    public static void main(String[] args) {
        if (args.length < 4) {
            logger.error("Please given the arguments java -jar Main.jar abstract [patternOriPath] [jsonPath] [PatternAbsPath]");
            return;
        }

        Path patternOriPath = Path.of(args[1]);
        Path jsonPath = Path.of(args[2]);
        Path patternAbsPath = Path.of(args[3]);

        Optional<Pattern> patternOri = Serializer.deserializeFromDisk(patternOriPath);
        if (patternOri.isEmpty()) {
            logger.error("Failed to read pattern from: " + patternOriPath);
            return;
        }
        Pattern pattern = patternOri.get();

        Abstractor abstractor = new LLMAbstractor(jsonPath);
        abstractor.doAbstraction(pattern);

        Serializer.serializeToDisk(pattern, patternAbsPath);
    }

}

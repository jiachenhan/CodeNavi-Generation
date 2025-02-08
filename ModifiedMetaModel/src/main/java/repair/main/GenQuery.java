package repair.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.dsl.kirin.QueryGenerator;
import repair.dsl.kirin.query.Query;
import repair.pattern.Pattern;
import repair.pattern.serialize.Serializer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class GenQuery {
    private final static Logger logger = LoggerFactory.getLogger(GenQuery.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            logger.error("Please given the arguments java -jar Main.jar genquery [patternPath] [queryPath]");
        }
        Path patternPath = Path.of(args[1]);
        Path queryPath = Path.of(args[2]);
        logger.info("GenQuery from pattern: {} to: {}", patternPath, queryPath);
        try {
            Optional<Pattern> patternOpt = Serializer.deserializeFromDisk(patternPath);
            if (patternOpt.isEmpty()) {
                logger.error("Failed to read pattern from: {}", patternPath);
                return;
            }
            Pattern pattern = patternOpt.get();
            Query query = QueryGenerator.getInstance().generate(pattern);
            String prettyPrint = query.prettyPrint();
            FileUtils.ensureDirectoryExists(queryPath);
            Files.writeString(queryPath, prettyPrint);
        } catch (Exception e) {
            logger.error("Failed to gen query", e);
        }

    }
}

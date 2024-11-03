package repair.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.apply.det.Detector;
import repair.common.CodeChangeInfo;
import repair.common.CodeChangeInfoReader;
import repair.pattern.Pattern;
import repair.pattern.serialize.Serializer;

import java.nio.file.Path;
import java.util.Optional;

public class Detect {
    private final static Logger logger = LoggerFactory.getLogger(Detect.class);

    public static void main(String[] args) {
        if (args.length < 5) {
            logger.error("Please given the arguments java -jar Main.jar detect " +
                    "[patternPath] [repoPath] [buggyInfoPath] [resultPath]");
            return;
        }

        Path patternPath = Path.of(args[1]);
        Path repoPath = Path.of(args[2]);
        Path buggyInfoPath = Path.of(args[3]);
        Path resultPath = Path.of(args[4]);

        logger.info("patternPath: " + patternPath + "\t buggyInfoPath: " + buggyInfoPath);
        try {

            // 1. 生成/抽象pattern
            Optional<Pattern> patternOpt = Serializer.deserializeFromDisk(patternPath);
            if (patternOpt.isEmpty()) {
                logger.error("Failed to read pattern from: " + patternPath);
                return;
            }
            Pattern pattern = patternOpt.get();

            // 2. 获取对应buggy info
            CodeChangeInfo buggyInfo = CodeChangeInfoReader.readCCInfo(buggyInfoPath);
            if(buggyInfo == null) {
                logger.error("Failed to read buggy info from: " + buggyInfoPath);
                return;
            }

            // 3. 遍历对应commit检测pattern
            Detector detector = new Detector(pattern, repoPath, buggyInfo.getBeforeCommitId(),
                    buggyInfo.getFilePath(),
                    buggyInfo.getSignatureBefore());
            detector.detect();
            detector.serializeResults(resultPath);

        } catch (Exception e) {
            logger.error("Failed to detect pattern", e);
        }

    }

}

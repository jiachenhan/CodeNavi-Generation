package repair.main;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.apply.det.Detector;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.apply.apr.ApplyModification;
import repair.apply.apr.ModificationException;
import repair.apply.match.MatchInstance;
import repair.apply.match.Matcher;
import repair.common.CodeChangeInfo;
import repair.common.CodeChangeInfoReader;
import repair.pattern.Pattern;
import repair.pattern.abstraction.Abstractor;
import repair.pattern.abstraction.LLMAbstractor;
import repair.pattern.abstraction.TermFrequencyAbstractor;
import repair.pattern.serialize.Serializer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.fail;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;
import static repair.common.Utils.generatePattern;

public class GenPat {
    private final static Logger logger = LoggerFactory.getLogger(GenPat.class);

    public static void repair_main(String[] args) {
        if (args.length < 4) {
            logger.error("Please given the arguments java -jar Main.jar genpat [patternPair] [buggyPair] [patchPath]");
            return;
        }

        Path patternPath = Path.of(args[1]);

        Path buggyPath = Path.of(args[2]);
        Path buggyBeforePath = buggyPath.resolve("before.java");
        Path patchPath = Path.of(args[3]);
        try {
            Pattern pattern = generatePattern(patternPath);

            Abstractor abstractor = new TermFrequencyAbstractor();
            abstractor.doAbstraction(pattern);

            CompilationUnit beforeCompilationUnit = genASTFromFile(buggyBeforePath);
            Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
            if(methodBefore.isEmpty()) {
                logger.error("MethodDeclaration is not present");
                return;
            }

            NodeParser beforeParser = new NodeParser(buggyBeforePath, beforeCompilationUnit);
            MoNode moMethodBefore = beforeParser.process(methodBefore.get());

            List<MatchInstance> matchInstances = Matcher.match(pattern, moMethodBefore).stream().limit(5).toList();
            if(matchInstances.isEmpty()) {
                System.out.println("No match found in " + buggyPath);

            }

            for (MatchInstance matchInstance : matchInstances) {
                if(!matchInstance.isLegal()) {
                    continue;
                }
                ApplyModification applyModification = new ApplyModification(pattern, moMethodBefore, matchInstance);

                try {
                    applyModification.apply();
                } catch (ModificationException e) {
                    logger.warn("apply error: {}", e.getMessage());
                }

                String afterCopyCode = "class PlaceHold {" + applyModification.getRight().toString() + "}";

                Path patchFilePath = patchPath.resolve(matchInstances.indexOf(matchInstance) + ".java");
                FileUtils.ensureDirectoryExists(patchFilePath.getParent());
                writeStringToFile(patchFilePath.toFile(), afterCopyCode, "UTF-8", false);
            }

        } catch (Exception e) {
            logger.error("build graph error: {}", e.getMessage());
        }
    }

    public static void detect_main(String[] args) {
        if (args.length < 6) {
            logger.error("Please given the arguments java -jar Main.jar genpat_detect " +
                    "[patternPair] [patternInfoPath] [repoPath] [buggyInfoPath] [resultPath]");
            return;
        }

        Path patternPath = Path.of(args[1]);
        Path patternInfoPath = Path.of(args[2]);
        Path repoPath = Path.of(args[3]);
        Path buggyInfoPath = Path.of(args[4]);
        Path resultPath = Path.of(args[5]);

        logger.info("patternPath: " + patternPath + "\t buggyInfoPath: " + buggyInfoPath);
        try {
            CodeChangeInfo patternInfo = CodeChangeInfoReader.readCCInfo(patternInfoPath);
            if (patternInfo == null) {
                logger.error("Failed to read pattern info from: " + patternInfoPath);
                return;
            }

            // 1. 生成/抽象pattern
            Pattern pattern = generatePattern(patternPath, patternInfo.getSignatureBefore(), patternInfo.getSignatureAfter());
            Abstractor abstractor = new TermFrequencyAbstractor();
            abstractor.doAbstraction(pattern);

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

    public static void abstract_main(String[] args) {
        if (args.length < 3) {
            logger.error("Please given the arguments java -jar Main.jar genpat_ab [patternOriPath] [PatternAbsPath]");
            return;
        }

        Path patternOriPath = Path.of(args[1]);
        Path patternAbsPath = Path.of(args[2]);

        Optional<Pattern> patternOri = Serializer.deserializeFromDisk(patternOriPath);
        if (patternOri.isEmpty()) {
            logger.error("Failed to read pattern from: {}", patternOriPath);
            return;
        }
        Pattern pattern = patternOri.get();

        Abstractor abstractor = new TermFrequencyAbstractor();
        abstractor.doAbstraction(pattern);

        Serializer.serializeToDisk(pattern, patternAbsPath);
    }

}

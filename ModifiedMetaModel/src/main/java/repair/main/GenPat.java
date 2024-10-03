package repair.main;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.modify.apply.ApplyModification;
import repair.modify.apply.ModificationException;
import repair.modify.apply.match.MatchInstance;
import repair.modify.apply.match.Matcher;
import repair.modify.diff.DiffComparator;
import repair.pattern.Pattern;
import repair.pattern.abstraction.Abstractor;
import repair.pattern.abstraction.TermFrequencyAbstractor;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.fail;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;
import static repair.main.Main.generatePattern;

public class GenPat {
    private final static Logger logger = LoggerFactory.getLogger(GenPat.class);

    public static void main(String[] args) {
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

}

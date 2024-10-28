package repair.main;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.apply.diff.DiffComparator;
import repair.common.MethodSignature;
import repair.pattern.Pattern;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.fail;
import static repair.common.JDTUtils.*;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please given the arguments");
            System.err.println("\tgenpat : ");
            System.exit(1);
        }

        switch (args[0]) {
            case "genpat":
                GenPat.repair_main(args);
                break;
            case "genpat_detect":
                GenPat.detect_main(args);
                break;
            case "oracle":
                GainOracle.main(args);
                break;
            case "extract":
                Extract.main(args);
                break;
            case "abstract":
                break;
            default:
                logger.error("not supported command: {}", args[0]);
        }
    }

    public static Pattern generatePattern(Path patternCase) {
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

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
    }

    public static Pattern generatePattern(Path patternCase, String beforeSignature, String afterSignature) {
        Path patternBeforePath = patternCase.resolve("before.java");
        Path patternAfterPath = patternCase.resolve("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(patternBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(patternAfterPath);

        MethodSignature methodSignatureBefore = MethodSignature.parseFunctionSignature(beforeSignature);
        MethodSignature methodSignatureAfter = MethodSignature.parseFunctionSignature(afterSignature);

        Optional<MethodDeclaration> methodBefore = getDeclaration(beforeCompilationUnit, methodSignatureBefore);
        Optional<MethodDeclaration> methodAfter = getDeclaration(afterCompilationUnit, methodSignatureAfter);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(patternBeforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(patternAfterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
    }
}

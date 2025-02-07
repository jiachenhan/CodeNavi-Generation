package repair.common;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import repair.apply.diff.DiffComparator;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.pattern.Pattern;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.fail;
import static repair.common.JDTUtils.*;
import static repair.common.JDTUtils.getMethodDeclaration;

public class Utils {
    public static Pattern generatePattern(Path patternCase) {
        System.out.println("Processing case: " + patternCase.getFileName());
        Path patternBeforePath = patternCase.resolve("before.java");
        Path patternAfterPath = patternCase.resolve("after.java");

        return generatePattern(patternBeforePath, patternAfterPath);
    }



    public static Pattern generatePattern(Path patternCase, String beforeSignature, String afterSignature) {
        Path patternBeforePath = patternCase.resolve("before.java");
        Path patternAfterPath = patternCase.resolve("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(patternBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(patternAfterPath);

        MethodSignature methodSignatureBefore = MethodSignature.parseFunctionSignature(beforeSignature);
        MethodSignature methodSignatureAfter = MethodSignature.parseFunctionSignature(afterSignature);

        Optional<MethodDeclaration> methodBefore = getMethodDeclaration(beforeCompilationUnit, methodSignatureBefore);
        Optional<MethodDeclaration> methodAfter = getMethodDeclaration(afterCompilationUnit, methodSignatureAfter);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(patternBeforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(patternAfterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
    }

    public static Pattern generatePattern(Path beforePath, Path afterPath) {
        CompilationUnit beforeCompilationUnit = genASTFromFile(beforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(afterPath);

        Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
        Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(beforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(afterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
    }


}

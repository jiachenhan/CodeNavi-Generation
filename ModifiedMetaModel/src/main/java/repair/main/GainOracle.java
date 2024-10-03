package repair.main;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class GainOracle {
    private final static Logger logger = LoggerFactory.getLogger(GenPat.class);

    public static void main(String[] args) {
        if (args.length < 4) {
            logger.error("Please given the arguments java -jar Main.jar oracle [fixedPath] [signature] [oraclePath]");
            return;
        }

        Path fixedPath = Path.of(args[1]);
        String signature = args[2];
        Path oraclePath = Path.of(args[3]);
        logger.info("fixedPath: {}, signature: {}, oraclePath: {}", fixedPath, signature, oraclePath);

        CompilationUnit beforeCompilationUnit = genASTFromFile(fixedPath);
        Optional<MethodDeclaration> method = Optional.empty();

        if("null".equals(signature)) {
            method = getOnlyMethodDeclaration(beforeCompilationUnit);
        } else {
            logger.error("to be implemented");
        }

        if(method.isEmpty()) {
            logger.error("MethodDeclaration is not present");
            return;
        }

        NodeParser parser = new NodeParser(fixedPath, beforeCompilationUnit);
        MoNode oracle = parser.process(method.get());

        System.out.println(oracle.toString());
        try {
            FileUtils.ensureDirectoryExists(oraclePath.getParent());
            writeStringToFile(oraclePath.toFile(), oracle.toString(), "UTF-8", false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

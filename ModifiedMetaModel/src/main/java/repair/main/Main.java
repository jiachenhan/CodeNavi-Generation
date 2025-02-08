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
            case "genpat" -> GenPat.repair_main(args);
            case "genpat_detect" -> GenPat.detect_main(args);
            case "oracle" -> GainOracle.main(args);
            case "extract" -> Extract.main(args);
            case "abstract" -> Abstract.main(args);
            case "detect" -> Detect.main(args);
            case "genquery" -> GenQuery.main(args);
            default -> logger.error("not supported command: {}", args[0]);
        }
    }


}

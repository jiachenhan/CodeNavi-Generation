package repair.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                GenPat.main(args);
                break;
            case "abstract":
                break;
            default:
                logger.error("not supported command: {}", args[0]);
        }
    }
}

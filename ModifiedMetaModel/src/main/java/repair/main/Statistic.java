package repair.main;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.apply.diff.operations.*;
import repair.common.Utils;
import repair.pattern.Pattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class Statistic {
    private final static Logger logger = LoggerFactory.getLogger(Statistic.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            logger.error("Please given the arguments java -jar Main.jar statistic [bugPath] [fixPath]");
            return;
        }

        Path bugPath = Path.of(args[1]);
        Path fixPath = Path.of(args[2]);


        // 统计操作粒度的复杂度
        Pattern pattern = Utils.generatePattern(bugPath, fixPath);

        int insert = 0, delete = 0, update = 0, move = 0;
        for (Operation<?> action : pattern.getAllOperations()) {
            if (action instanceof InsertOperation || action instanceof TreeInsertOperation) insert++;
            else if (action instanceof DeleteOperation || action instanceof TreeDeleteOperation) delete++;
            else if (action instanceof UpdateOperation) update++;
            else if (action instanceof MoveOperation) move++;
        }
        System.out.println("Insert: " + insert);
        System.out.println("Delete: " + delete);
        System.out.println("Update: " + update);
        System.out.println("Move: " + move);
        System.out.println("Total edits: " + pattern.getAllOperations().size());


        // 统计行粒度复杂度
        List<String> original = null;
        List<String> revised = null;
        try {
            original = Files.readAllLines(bugPath);
            revised = Files.readAllLines(fixPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Patch<String> patch = DiffUtils.diff(original, revised);

        int addedLines = 0;
        int deletedLines = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            deletedLines += delta.getSource().size();
            addedLines += delta.getTarget().size();
        }

        System.out.println("Added: " + addedLines);
        System.out.println("Deleted: " + deletedLines);
        System.out.println("Total changed: " + (addedLines + deletedLines));
    }

}

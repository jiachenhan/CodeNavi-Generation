package repair.modify.diff.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;

public class NaiveIndexStrategy implements InsertListStrategy {
    private static final Logger logger = LoggerFactory.getLogger(NaiveIndexStrategy.class);
    private final AddOperator addOperator;

    public NaiveIndexStrategy(AddOperator addOperator) {
        this.addOperator = addOperator;
    }

    @Override
    public int computeInsertIndex() {
        MoNode inserteeNodeInAfter = this.addOperator.getAddNode();
        MoNode insertParentInAfter = inserteeNodeInAfter.getParent();
        String role = this.addOperator.getLocation().role();
        Object structuralProperty = insertParentInAfter.getStructuralProperty(role);
        if(structuralProperty instanceof MoNodeList<?> insertList) {
            int index = insertList.indexOf(inserteeNodeInAfter);
            if (index != -1) {
                return index;
            }
            logger.error("Insertee node not found in the insert list");
        } else{
            logger.error("Insert location is not a list");
        }
        return 0;
    }
}

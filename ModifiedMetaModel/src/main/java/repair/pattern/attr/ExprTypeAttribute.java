package repair.pattern.attr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.code.expression.MoExpression;

import java.io.Serial;

public class ExprTypeAttribute extends Attribute<String> implements HardConstraint {
    private static final Logger logger = LoggerFactory.getLogger(ExprTypeAttribute.class);
    @Serial
    private static final long serialVersionUID = -1586388608688461868L;

    // 用?表示不确定的类型,这种类型都可以适配
    public ExprTypeAttribute(MoNode node) {
        super(node);
        if(node instanceof MoExpression expression) {
            this.value = expression.getExprTypeStr();
        } else {
            this.value = "UNCompatible";
        }
        super.considered = true;
    }

    @Override
    public double similarity(Attribute<?> other) {
        if (other instanceof ExprTypeAttribute exprTypeAttribute) {
            if(this.value.equals(MoExpression.UnknownType) || exprTypeAttribute.value.equals(MoExpression.UnknownType)) {
                return 1;
            }
            return this.value.equals(exprTypeAttribute.value) ? 1 : -1;
        }
        logger.error("Cannot compare ExprTypeAttribute with " + other.getClass());
        return -1;
    }

}

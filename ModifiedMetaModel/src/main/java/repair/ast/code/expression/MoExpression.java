package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import repair.ast.MoNode;

import java.io.Serial;


public abstract class MoExpression extends MoNode {
    @Serial
    private static final long serialVersionUID = -2128799087068278887L;

    private String ExprTypeStr;
    public static final String UnknownType = "<UNKNOWN>";
    protected MoExpression(String fileName, int startLine, int endLine, Expression expression) {
        super(fileName, startLine, endLine, expression);
        if(expression == null) {
            // 说明是clone出来的，没有expression，可以先设置为unknown
            // todo: 设置拷贝的表达式类型
            setExprTypeStr(UnknownType);
            return;
        }
        ITypeBinding typeBinding = expression.resolveTypeBinding();
        if (typeBinding != null) {
            setExprTypeStr(typeBinding.getName());
        } else {
            setExprTypeStr(UnknownType);
        }
    }

    public void setExprTypeStr(String exprTypeStr) {
        ExprTypeStr = exprTypeStr;
    }

    public String getExprTypeStr() {
        return ExprTypeStr;
    }
}
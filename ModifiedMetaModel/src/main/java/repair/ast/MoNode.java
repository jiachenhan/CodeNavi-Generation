package repair.ast;

import org.eclipse.jdt.core.dom.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.behavior.NodeComparator;
import repair.ast.code.context.Context;
import repair.ast.behavior.Visitable;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.visitor.CodePrinter;
import repair.ast.visitor.TokenizeScanner;


import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MoNode implements Visitable, Serializable, NodeComparator {
    private static final Logger logger = LoggerFactory.getLogger(MoNode.class);
    @Serial
    private static final long serialVersionUID = -6674846168396567373L;

    protected static final AtomicInteger counter = new AtomicInteger(0);
    protected final int id;
    public static void resetCounter() {
        counter.set(0);
    }


    /**
     * @param fileName  : source file name (with absolute path)
     * @param startLine : start line number of the node in the original source file
     * @param endLine   : end line number of the node in the original source file
     * @param elementPos : start position of the node in the original source file
     * @param elementLength : length of the node in the original source file
     * @param oriNode   : original abstract syntax tree node in the JDT model
     */
    public MoNode(String fileName, int startLine, int endLine, int elementPos, int elementLength, ASTNode oriNode) {
        id = counter.incrementAndGet();

        this.fileName = fileName;
        this.startLine = startLine;
        this.endLine = endLine;
        this.elementPos = elementPos;
        this.elementLength = elementLength;
        this.oriNode = oriNode;
    }

    public MoNode(String fileName, int startLine, int endLine, ASTNode oriNode) {
        id = counter.incrementAndGet();

        this.fileName = fileName;
        this.startLine = startLine;
        this.endLine = endLine;
        this.oriNode = oriNode;
        if(oriNode != null) {
            this.elementPos = oriNode.getStartPosition();
            this.elementLength = oriNode.getLength();
        } else {
            this.elementPos = -1;
            this.elementLength = -1;
        }
    }

    /*
     * original File info
     */

    /**
     * source file name (with absolute path)
     */
    private final String fileName;

    public String getFileName() {
        return fileName;
    }

    private final int startLine;
    private final int endLine;
    private final int elementPos;
    private final int elementLength;
    public int getElementPos() {
        return elementPos;
    }
    public int getElementLength() {
        return elementLength;
    }

    /**
     * parent node in the abstract syntax tree
     */
    private MoNode parent = null;
    private Description<? extends MoNode, ?> location = null;

    public final void setParent(MoNode parent, Description<? extends MoNode, ?> locationInParent) {
        this.parent = parent;
        this.location = locationInParent;
    }
    public final MoNode getParent() {
        return parent;
    }
    public final Description<? extends MoNode, ?> getLocationInParent() {
        return location;
    }
    public abstract List<MoNode> getChildren(); // 获取直接子节点，如果没有子节点，返回空列表
    public abstract boolean isLeaf(); // 是否是叶子节点

    // 通过一个节点的role，获取对应的属性值（子节点）
    public abstract Object getStructuralProperty(String role);
    public abstract void setStructuralProperty(String role, Object value);

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    /**
     * 向this节点的childList类型的属性中添加一个元素, 例如：添加一个参数到方法的参数列表中
     * this节点的role对应的属性必须是childList类型
     * @param role : childList类型节点的role
     * @param element : 要添加的元素
     * @param <T> : 元素的类型
     */
    @SuppressWarnings("unchecked")
    public <T> void addStructuralPropertyList(String role, T element) {
        Object parentStructuralPropertyList = this.getStructuralProperty(role);
        if (parentStructuralPropertyList instanceof MoNodeList<?>) {
            MoNodeList<T> list = (MoNodeList<T>) parentStructuralPropertyList;
            list.setParent(this);
            list.add(element);
        } else {
            throw new ClassCastException("Object is not a List");
        }
    }

    /**
     * 通过一个节点的role，获取对应的描述
     */
    public abstract Description<? extends MoNode, ?> getDescription(String role);

    /**
     * enum type of node, for easy comparison
     */
    protected MoNodeType moNodeType = MoNodeType.UNKNOWN;
    public MoNodeType getMoNodeType() {
        return moNodeType;
    }

    /**
     * original AST node in the JDT abstract tree model
     * NOTE: AST node dose not support serialization
     */
    protected transient ASTNode oriNode;

    /**
     *  Context infos
     */
    public Context context = new Context();

    /**
     * output source code with string format
     *
     * @return : source code string
     */
    public String toSrcString() {
        CodePrinter codePrinter = new CodePrinter();
        codePrinter.scan(this);
        return codePrinter.getCode();
    }

    public List<String> tokens() {
        TokenizeScanner scanner = new TokenizeScanner();
        scanner.scan(this);
        return scanner.getTokens();
    }

    public void removeFromParent() {
        if(parent == null) {
            logger.error("Parent is null");
            return;
        }
        if (getLocationInParent().classification() == ChildType.CHILDLIST) {
            MoNodeList<?> structuralProperty = (MoNodeList<?>) parent.getStructuralProperty(location.role());
            structuralProperty.remove(this);
        } else if(getLocationInParent().classification() == ChildType.CHILD) {
            if (parent.getStructuralProperty(location.role()) == this) {
                parent.setStructuralProperty(location.role(), null);
            }
        } else {
            logger.error("Unknown child type");
        }
        this.setParent(null, null);
    }

    public abstract MoNode shallowClone();

    @Override
    public String toString() {
        return toSrcString();
    }

}

package repair.ast;

import repair.ast.code.MoDocElement;
import repair.ast.role.Description;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @param <V> is a subclass of MoNode
 */
public class MoNodeList<V> extends AbstractList<V> implements Serializable {
    @Serial
    private static final long serialVersionUID = 5759016222425629792L;
    ArrayList<V> store = new ArrayList<>(0);
    private MoNode parent;
    // 将相同的父子关系引入兄弟节点
    Description<? extends MoNode, V> description;

    public MoNodeList(Description<? extends MoNode, V> description) {
        super();
        this.description = description;
    }

    public void setParent(MoNode parent) {
        if(this.parent == null) {
            this.parent = parent;
            return;
        }
        if (this.parent != parent) {
            throw new IllegalStateException("children have different parents");
        }
    }

    public MoNodeList(MoNode parent, Description<? extends MoNode, V> description) {
        super();
        this.parent = parent;
        this.description = description;
    }


    public int size() {
        return this.store.size();
    }

    public V get(int index) {
        return this.store.get(index);
    }

    @Override
    public V set(int index, V newChild) {
        V oldChild = this.store.get(index);
        if (oldChild == newChild) {
            return oldChild;
        }

        ((MoNode) oldChild).setParent(null, null);
        ((MoNode) newChild).setParent(parent, this.description);
        return this.store.set(index, newChild);
    }

    public void replace(int index, V newChild) {
        set(index, newChild);
    }

    @Override
    public boolean add(V newChild) {
        ((MoNode) newChild).setParent(parent, this.description);
        this.store.add(newChild);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof MoNode) {
            ((MoNode) o).setParent(null, null);
        }
        return this.store.remove(o);
    }

    @Override
    public void add(int index, V newChild) {
        ((MoNode) newChild).setParent(parent, this.description);
        this.store.add(index, newChild);
    }

    @Override
    public V remove(int index) {
        V oldChild = this.store.remove(index);
        ((MoNode) oldChild).setParent(null, null);
        return oldChild;
    }

    public Description<? extends MoNode, V> getDescription() {
        return description;
    }

    public static boolean sameList(List<? extends MoNode> list1, List<? extends MoNode> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).isSame(list2.get(i))) {
                return false;
            }
        }
        return true;
    }

}

package repair.ast.declaration;

import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoDimension;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MoVariableDeclarationFragment extends MoVariableDeclaration {
    private static final Logger logger = LoggerFactory.getLogger(MoVariableDeclarationFragment.class);

    @Serial
    private static final long serialVersionUID = 3859608733693556563L;

    private final static Description<MoVariableDeclarationFragment, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoVariableDeclarationFragment.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoVariableDeclarationFragment, MoDimension> extraDimensionsDescription =
            new Description<>(ChildType.CHILDLIST, MoVariableDeclarationFragment.class, MoDimension.class,
                    "extraDimensions2", true);

    private final static Description<MoVariableDeclarationFragment, MoExpression> initializerDescription =
            new Description<>(ChildType.CHILD, MoVariableDeclarationFragment.class, MoExpression.class,
                    "initializer", false);

    private final static Map<String, Description<MoVariableDeclarationFragment, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("name", nameDescription),
            Map.entry("extraDimensions2", extraDimensionsDescription),
            Map.entry("initializer", initializerDescription)
    );


    public MoVariableDeclarationFragment(Path fileName, int startLine, int endLine, VariableDeclarationFragment variableDeclarationFragment) {
        super(fileName, startLine, endLine, variableDeclarationFragment);
        moNodeType = MoNodeType.TYPEVariableDeclarationFragment;
        super.CStyleArrayDimensions = new MoNodeList<>(this, extraDimensionsDescription);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoVariableDeclarationFragment(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        children.add(name);
        children.addAll(CStyleArrayDimensions);
        if(initializer != null) {
            children.add(initializer);
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoVariableDeclarationFragment, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            return super.name;
        } else if(description == extraDimensionsDescription) {
            return super.CStyleArrayDimensions;
        } else if(description == initializerDescription) {
            return super.initializer;
        } else {
            logger.error("Role {} not found in MoVariableDeclarationFragment", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoVariableDeclarationFragment, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            super.name = (MoSimpleName) value;
        } else if(description == extraDimensionsDescription) {
            super.CStyleArrayDimensions.clear();
            super.CStyleArrayDimensions.addAll((MoNodeList<MoDimension>) value);
        } else if(description == initializerDescription) {
            super.initializer = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoVariableDeclarationFragment", role);
        }
    }

    public static Map<String, Description<MoVariableDeclarationFragment, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoVariableDeclarationFragment(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoVariableDeclarationFragment moVariableDeclarationFragment) {
            boolean match = name.isSame(moVariableDeclarationFragment.name);
            match = match && MoNodeList.sameList(CStyleArrayDimensions, moVariableDeclarationFragment.CStyleArrayDimensions);
            if(initializer == null) {
                return match && moVariableDeclarationFragment.initializer == null;
            } else {
                return match && initializer.isSame(moVariableDeclarationFragment.initializer);
            }
        }
        return false;
    }
}

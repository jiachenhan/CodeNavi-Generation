package repair.ast.declaration;

import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.code.MoJavadoc;
import repair.ast.code.expression.MoName;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.*;

public class MoPackageDeclaration extends MoNode {
    private static final Logger logger = LoggerFactory.getLogger(MoPackageDeclaration.class);
    @Serial
    private static final long serialVersionUID = -529231313418164413L;

    private final static Description<MoPackageDeclaration, MoJavadoc> javadocDescription =
            new Description<>(ChildType.CHILD, MoPackageDeclaration.class, MoJavadoc.class,
                    "javadoc", false);

    private final static Description<MoPackageDeclaration, MoAnnotation> annotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoPackageDeclaration.class, MoAnnotation.class,
                    "annotations", true);

    private final static Description<MoPackageDeclaration, MoName> nameDescription =
            new Description<>(ChildType.CHILD, MoPackageDeclaration.class, MoName.class,
                    "name", true);

    private final static Map<String, Description<MoPackageDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("javadoc", javadocDescription),
            Map.entry("annotations", annotationsDescription),
            Map.entry("name", nameDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "javadoc", mandatory = false)
    private MoJavadoc javadoc;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "annotations", mandatory = true)
    private final MoNodeList<MoAnnotation> annotations;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoName name;

    public MoPackageDeclaration(String fileName, int startLine, int endLine, PackageDeclaration packageDeclaration) {
        super(fileName, startLine, endLine, packageDeclaration);
        moNodeType = MoNodeType.TYPEPackageDeclaration;
        annotations = new MoNodeList<>(this, annotationsDescription);
    }

    public void setJavadoc(MoJavadoc javadoc) {
        this.javadoc = javadoc;
    }

    public void addAnnotation(MoAnnotation annotation) {
        annotations.add(annotation);
    }

    public void setName(MoName name) {
        this.name = name;
    }

    public Optional<MoJavadoc> getJavadoc() {
        return Optional.ofNullable(javadoc);
    }

    public MoNodeList<MoAnnotation> getAnnotations() {
        return annotations;
    }

    public MoName getName() {
        return name;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoPackageDeclaration(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        if(javadoc != null) {
            children.add(javadoc);
        }
        children.addAll(annotations);
        children.add(name);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoPackageDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            return javadoc;
        } else if(description == annotationsDescription) {
            return annotations;
        } else if(description == nameDescription) {
            return name;
        } else {
            logger.error("Role {} not found in MoPackageDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoPackageDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            this.javadoc = (MoJavadoc) value;
        } else if(description == annotationsDescription) {
            annotations.clear();
            annotations.addAll((List<MoAnnotation>) value);
        } else if(description == nameDescription) {
            this.name = (MoName) value;
        } else {
            logger.error("Role {} not found in MoPackageDeclaration", role);
        }
    }

    public static Map<String, Description<MoPackageDeclaration, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoPackageDeclaration(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoPackageDeclaration otherPackageDeclaration) {
            boolean match;
            if(javadoc == null) {
                match = otherPackageDeclaration.javadoc == null;
            } else {
                match = javadoc.isSame(otherPackageDeclaration.javadoc);
            }
            match = match && MoExtendedModifier.sameList(annotations, otherPackageDeclaration.annotations);
            match = match && name.isSame(otherPackageDeclaration.name);
            return match;
        }
        return false;
    }
}

package repair.ast;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.declaration.*;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoCompilationUnit extends MoNode implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MoCompilationUnit.class);
    @Serial
    private static final long serialVersionUID = -657327222264409056L;

    private final static Description<MoCompilationUnit, MoPackageDeclaration> packageDeclarationDescription =
            new Description<>(ChildType.CHILD, MoCompilationUnit.class, MoPackageDeclaration.class,
                    "package", false);

    private final static Description<MoCompilationUnit, MoImportDeclaration> importsDescription =
            new Description<>(ChildType.CHILDLIST, MoCompilationUnit.class, MoImportDeclaration.class,
                    "imports", true);

    private final static Description<MoCompilationUnit, MoAbstractTypeDeclaration> typesDescription =
            new Description<>(ChildType.CHILDLIST, MoCompilationUnit.class, MoAbstractTypeDeclaration.class,
                    "types", true);

    private final static Map<String, Description<MoCompilationUnit, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("package", packageDeclarationDescription),
            Map.entry("imports", importsDescription),
            Map.entry("types", typesDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "package", mandatory = false)
    private MoPackageDeclaration packageDeclaration;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "imports", mandatory = true)
    private final MoNodeList<MoImportDeclaration> imports;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "types", mandatory = true)
    private final MoNodeList<MoAbstractTypeDeclaration> types;


    public MoCompilationUnit(String fileName, int startLine, int endLine, CompilationUnit oriNode) {
        super(fileName, startLine, endLine, oriNode);
        moNodeType = MoNodeType.TYPECompilationUnit;
        imports = new MoNodeList<>(this, importsDescription);
        types = new MoNodeList<>(this, typesDescription);
    }

    public void setPackageDeclaration(MoPackageDeclaration packageDeclaration) {
        this.packageDeclaration = packageDeclaration;
    }

    public void addImport(MoImportDeclaration moImportDeclaration) {
        imports.add(moImportDeclaration);
    }

    public Optional<MoPackageDeclaration> getPackageDeclaration() {
        return Optional.ofNullable(packageDeclaration);
    }

    public List<MoImportDeclaration> getImports() {
        return imports;
    }

    public List<MoAbstractTypeDeclaration> getTypes() {
        return types;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoCompilationUnit(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoCompilationUnit, ?> description = descriptionsMap.get(role);
        if(description == packageDeclarationDescription) {
            return packageDeclaration;
        } else if(description == importsDescription) {
            return imports;
        } else if(description == typesDescription) {
            return types;
        } else {
            logger.error("Role {} not found in MoCompilationUnit", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoCompilationUnit, ?> description = descriptionsMap.get(role);
        if(description == packageDeclarationDescription) {
            packageDeclaration = (MoPackageDeclaration) value;
        } else if(description == importsDescription) {
            imports.clear();
            imports.addAll((List<MoImportDeclaration>) value);
        } else if(description == typesDescription) {
            types.clear();
            types.addAll((List<MoAbstractTypeDeclaration>) value);
        } else {
            logger.error("Role {} not found in MoCompilationUnit", role);
        }
    }

    public static Map<String, Description<MoCompilationUnit, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoCompilationUnit(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoCompilationUnit moCompilationUnit) {
            return ( (packageDeclaration == null && moCompilationUnit.packageDeclaration == null) ||
                    (packageDeclaration != null && packageDeclaration.isSame(moCompilationUnit.packageDeclaration)) ) &&
                    MoNodeList.sameList(imports, moCompilationUnit.imports) &&
                    MoNodeList.sameList(types, moCompilationUnit.types);
        }
        return false;
    }
}

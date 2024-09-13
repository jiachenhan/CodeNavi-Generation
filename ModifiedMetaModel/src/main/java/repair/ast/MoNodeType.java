package repair.ast;

/**
 * Node type model
 * all types of abstract syntax tree node considered currently
 */
public enum MoNodeType {
    TYPEAnonymousClassDeclaration("AnonymousClassDeclaration", 1),
    TYPEArrayAccess("ArrayAccess", 2),
    TYPEArrayCreation("ArrayCreation", 3),
    TYPEArrayInitializer("ArrayInitializer", 4),
    TYPEArrayType("ArrayType", 5),
    TYPEAssertStatement("AssertStatement", 6),
    TYPEAssignment("Assignment", 7),
    TYPEBlock("Block", 8),
    TYPEBooleanLiteral("BooleanLiteral", 9),
    TYPEBreakStatement("BreakStatement", 10),
    TYPECastExpression("CastExpression", 11),
    TYPECatchClause("CatchClause", 12),
    TYPECharacterLiteral("CharacterLiteral", 13),
    TYPEClassInstanceCreation("ClassInstanceCreation", 14),
    TYPECompilationUnit("CompilationUnit", 15),
    TYPEConditionalExpression("ConditionalExpression", 16),
    TYPEConstructorInvocation("ConstructorInvocation", 17),
    TYPEContinueStatement("ContinueStatement", 18),
    TYPEDoStatement("DoStatement", 19),
    TYPEEmptyStatement("EmptyStatement", 20),
    TYPEExpressionStatement("ExpressionStatement", 21),
    TYPEFieldAccess("FieldAccess", 22),
    TYPEFieldDeclaration("FieldDeclaration", 23),
    TYPEForStatement("ForStatement", 24),
    TYPEIfStatement("IfStatement", 25),
    TYPEImportDeclaration("ImportDeclaration", 26),
    TYPEInfixExpression("InfixExpression", 27),
    TYPEInitializer("Initializer", 28),
    TYPEJavadoc("Javadoc", 29),
    TYPELabeledStatement("LabeledStatement", 30),
    TYPEMethodDeclaration("MethodDeclaration", 31),
    TYPEMethodInvocation("MethodInvocation", 32),
    TYPENullLiteral("NullLiteral", 33),
    TYPENumberLiteral("NumberLiteral", 34),
    TYPEPackageDeclaration("PackageDeclaration", 35),
    TYPEParenthesizedExpression("ParenthesizedExpression", 36),
    TYPEPostfixExpression("PostfixExpression", 37),
    TYPEPrefixExpression("PrefixExpression", 38),
    TYPEPrimitiveType("PrimitiveType", 39),
    TYPEQualifiedName("QualifiedName", 40),
    TYPEReturnStatement("ReturnStatement", 41),
    TYPESimpleName("SimpleName", 42),
    TYPESimpleType("SimpleType", 43),
    TYPESingleVariableDeclaration("SingleVariableDeclaration", 44),
    TYPEStringLiteral("StringLiteral", 45),
    TYPESuperConstructorInvocation("SuperConstructorInvocation", 46),
    TYPESuperFieldAccess("SuperFieldAccess", 47),
    TYPESuperMethodInvocation("SuperMethodInvocation", 48),
    TYPESwitchCase("SwitchCase", 49),
    TYPESwitchStatement("SwitchStatement", 50),
    TYPESynchronizedStatement("SynchronizedStatement", 51),
    TYPEThisExpression("ThisExpression", 52),
    TYPEThrowStatement("ThrowStatement", 53),
    TYPETryStatement("TryStatement", 54),
    TYPETypeDeclaration("TypeDeclaration", 55),
    TYPETypeDeclarationStatement("TypeDeclarationStatement", 56),
    TYPETypeLiteral("TypeLiteral", 57),
    TYPEVariableDeclarationExpression("VariableDeclarationExpression", 58),
    TYPEVariableDeclarationFragment("VariableDeclarationFragment", 59),
    TYPEVariableDeclarationStatement("VariableDeclarationStatement", 60),
    TYPEWhileStatement("WhileStatement", 61),
    TYPEInstanceofExpression("InstanceofExpression", 62),
    TYPELineComment("LineComment", 63),
    TYPEBlockComment("BlockComment", 64),
    TYPETagElement("TagElement", 65),
    TYPETextElement("TextElement", 66),

//    TYPEMemberRef("MemberRef", 67),
//    TYPEMethodRef("MethodRef", 68),
//    TYPEMethodRefParameter("MethodRefParameter", 69),
    TYPEEnhancedForStatement("EnhancedForStatement", 70),
    TYPEEnumDeclaration("EnumDeclaration", 71),
    TYPEEnumConstantDeclaration("EnumConstantDeclaration", 72),
    TYPETypeParameter("TypeParameter", 73),
    TYPEParameterizedType("ParameterizedType", 74),
    TYPEQualifiedType("QualifiedType", 75),
    TYPEWildcardType("WildcardType", 76),
    TYPENormalAnnotation("NormalAnnotation", 77),
    TYPEMarkerAnnotation("MarkerAnnotation", 78),
    TYPESingleMemberAnnotation("SingleMemberAnnotation", 79),
    TYPEMemberValuePair("MemberValuePair", 80),
//    TYPEAnnotationTypeDeclaration("AnnotationTypeDeclaration", 81),
//    TYPEAnnotationTypeMemberDeclaration("AnnotationTypeMemberDeclaration", 82),
    TYPEModifier("Modifier", 83),
    TYPEUnionType("UnionType", 84),
    TYPEDimension("Dimension", 85),
    TYPELambdaExpression("LambdaExpression", 86),
    TYPEIntersectionType("IntersectionType", 87),
    TYPENameQualifiedType("NameQualifiedType", 88),
    TYPECreationReference("CreationReference", 89),
    TYPEExpressionMethodReference("ExpressionMethodReference", 90),
    TYPESuperMethodReference("SuperMethodReference", 91),
    TYPETypeMethodReference("TypeMethodReference", 92),

    // the following types are not in JDT
    TYPEAssigmentOperator("AssignmentOperator", -1),
    TYPEInfixOperator("InfixOperator", -1),
    TYPEPrefixOperator("PrefixOperator", -1),
    TYPEPostfixOperator("PostfixOperator", -1),

    UNKNOWN("Unknown", -1);

    private final String nodeType;
    private final int JDTIndex;

    MoNodeType(String nodeType, int JDTIndex) {
        this.nodeType = nodeType;
        this.JDTIndex = JDTIndex;
    }

    @Override
    public String toString() {
        return "JDTIndex: " + JDTIndex + "\n" +
                "NodeType: " + nodeType;
    }
}

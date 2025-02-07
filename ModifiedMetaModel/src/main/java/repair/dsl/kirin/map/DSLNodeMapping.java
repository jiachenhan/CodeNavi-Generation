package repair.dsl.kirin.map;

import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoTryStatement;
import repair.dsl.kirin.map.code.KeyWordFactory;
import repair.dsl.kirin.map.code.node.*;

import java.util.Map;

import static java.util.Map.entry;

public class DSLNodeMapping {
    public static final Map<MoNodeType, Class<? extends DSLNode>> nodeMapping = Map.<MoNodeType, Class<? extends DSLNode>>ofEntries(
            entry(MoNodeType.TYPEModifier, DSLUnSupportNode.class),
            entry(MoNodeType.TYPETypeParameter, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEPackageDeclaration, DSLUnSupportNode.class),
            entry(MoNodeType.TYPETextElement, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEJavadoc, DSLUnSupportNode.class),
            entry(MoNodeType.TYPELineComment, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEBlockComment, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEImportDeclaration, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEAnonymousClassDeclaration, AnonymousInnerClassExpression.class),
            entry(MoNodeType.TYPEExpressionStatement, DSLUnSupportNode.class),
            entry(MoNodeType.TYPETryStatement, ExceptionBlock.class),
            entry(MoNodeType.TYPEEnhancedForStatement, ForEachBlock.class),
            entry(MoNodeType.TYPESwitchStatement, SwitchBlock.class),
            entry(MoNodeType.TYPETypeDeclarationStatement, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEDoStatement, DoWhileBlock.class),
            entry(MoNodeType.TYPEReturnStatement, ReturnStatement.class),
            entry(MoNodeType.TYPEEmptyStatement, DSLUnSupportNode.class),
            entry(MoNodeType.TYPESuperConstructorInvocation, SuperCall.class),
            entry(MoNodeType.TYPEContinueStatement, ContinueStatement.class),
            entry(MoNodeType.TYPEConstructorInvocation, ThisCall.class),
            entry(MoNodeType.TYPEWhileStatement, WhileStatement.class),
            entry(MoNodeType.TYPEIfStatement, IfStatement.class),
            entry(MoNodeType.TYPEVariableDeclarationStatement, ValueDeclaration.class),
            entry(MoNodeType.TYPEBreakStatement, BreakStatement.class),
            entry(MoNodeType.TYPEAssertStatement, AssertStatement.class),
            entry(MoNodeType.TYPEForStatement, ForStatement.class),
            entry(MoNodeType.TYPESynchronizedStatement, SynchronizedBlock.class),
            entry(MoNodeType.TYPELabeledStatement, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEBlock, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEThrowStatement, ThrowStatement.class),
            entry(MoNodeType.TYPESwitchCase, CaseStatement.class),
            entry(MoNodeType.TYPETagElement, DSLUnSupportNode.class),
            entry(MoNodeType.TYPECatchClause, CatchBlock.class),
            entry(MoNodeType.TYPEDimension, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEMemberValuePair, AnnoMember.class),

            entry(MoNodeType.TYPEEnumConstantDeclaration, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEEnumDeclaration, DSLUnSupportNode.class),
            entry(MoNodeType.TYPETypeDeclaration, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEFieldDeclaration, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEInitializer, DSLUnSupportNode.class),

            entry(MoNodeType.TYPEMethodDeclaration, FunctionDeclaration.class),

            entry(MoNodeType.TYPEArrayType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPESimpleType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEPrimitiveType, PrimitiveType.class),
            entry(MoNodeType.TYPEQualifiedType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEWildcardType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPENameQualifiedType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEIntersectionType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEUnionType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEParameterizedType, DSLUnSupportNode.class),
            entry(MoNodeType.TYPECompilationUnit, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEVariableDeclarationFragment, DSLUnSupportNode.class),
            entry(MoNodeType.TYPESingleVariableDeclaration, ValueDeclaration.class),
            entry(MoNodeType.TYPEParenthesizedExpression, DSLUnSupportNode.class),
            entry(MoNodeType.TYPELambdaExpression, LambdaExpression.class),
            entry(MoNodeType.TYPEArrayAccess, ArrayAccess.class),
            entry(MoNodeType.TYPEStringLiteral, Literal.class),
            entry(MoNodeType.TYPECastExpression, CastExpression.class),
            entry(MoNodeType.TYPEInfixExpression, BinaryOperation.class),
            entry(MoNodeType.TYPEMethodInvocation, FunctionCall.class),
            entry(MoNodeType.TYPESuperMethodInvocation, FunctionCall.class),
            entry(MoNodeType.TYPEArrayCreation, ArrayCreation.class),
            entry(MoNodeType.TYPEPostfixExpression, UnaryOperation.class),
            entry(MoNodeType.TYPECharacterLiteral, Literal.class),
            entry(MoNodeType.TYPEBooleanLiteral, Literal.class),
            entry(MoNodeType.TYPEClassInstanceCreation, ObjectCreationExpression.class),
            entry(MoNodeType.TYPEAssignment, AssignStatement.class),
            entry(MoNodeType.TYPENormalAnnotation, Annotation.class),
            entry(MoNodeType.TYPEMarkerAnnotation, Annotation.class),
            entry(MoNodeType.TYPESingleMemberAnnotation, Annotation.class),
            entry(MoNodeType.TYPENullLiteral, NullLiteral.class),
            entry(MoNodeType.TYPENumberLiteral, Literal.class),
            entry(MoNodeType.TYPESuperMethodReference, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEExpressionMethodReference, MethodReferenceExpression.class),
            entry(MoNodeType.TYPECreationReference, MethodReferenceExpression.class),
            entry(MoNodeType.TYPETypeMethodReference, MethodReferenceExpression.class),
            entry(MoNodeType.TYPEThisExpression, ThisExpression.class),
            entry(MoNodeType.TYPESuperFieldAccess, FieldAccess.class),
            entry(MoNodeType.TYPEConditionalExpression, TernaryOperation.class),
            entry(MoNodeType.TYPEPrefixExpression, UnaryOperation.class),
            entry(MoNodeType.TYPEFieldAccess, FieldAccess.class),
            entry(MoNodeType.TYPEQualifiedName, Name.class),
            entry(MoNodeType.TYPESimpleName, Name.class),
            entry(MoNodeType.TYPEVariableDeclarationExpression, ValueDeclaration.class),
            entry(MoNodeType.TYPEInstanceofExpression, InstanceofExpression.class),
            entry(MoNodeType.TYPEArrayInitializer, InitArrayExpression.class),
            entry(MoNodeType.TYPETypeLiteral, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEPostfixOperator, Operator.class),
            entry(MoNodeType.TYPEAssigmentOperator, Operator.class),
            entry(MoNodeType.TYPEMethodInvocationTarget, DSLUnSupportNode.class),
            entry(MoNodeType.TYPEInfixOperator, Operator.class),
            entry(MoNodeType.TYPEPrefixOperator, Operator.class),
            entry(MoNodeType.TYPEMethodInvocationArguments, DSLUnSupportNode.class)
    );

    public static Class<? extends DSLNode> convertDSLNode(MoNode node) {
        if (node instanceof MoTryStatement tryStatement) {
            if (tryStatement.getResources().isEmpty()) {
                return ExceptionBlock.class;
            } else {
                return TryWithResources.class;
            }
        }
        return nodeMapping.get(node.getMoNodeType());
    }

    public static DSLNode convertMoNode2DSLNode(MoNode node) {
        Class<? extends DSLNode> dslNodeClass = convertDSLNode(node);
        return KeyWordFactory.createNodeInstance(dslNodeClass);
    }

}

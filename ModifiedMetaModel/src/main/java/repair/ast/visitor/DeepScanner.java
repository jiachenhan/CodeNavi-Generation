package repair.ast.visitor;

import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import repair.ast.code.*;
import repair.ast.code.expression.*;
import repair.ast.code.expression.literal.*;
import repair.ast.code.statement.*;
import repair.ast.code.type.*;
import repair.ast.code.virtual.*;
import repair.ast.declaration.*;

import java.util.Collection;
import java.util.List;

public class DeepScanner implements Visitor{
    public DeepScanner() {
    }

    /**
     * todo: if use this method, need to create a post-visitor for each node.
     * because the enter and exit method can not be called in the same time.
      */
    protected void enter(MoNode moNode) {
    }
    protected void exit(MoNode moNode) {
    }

    public void scan(String role, Collection<? extends MoNode> nodes) {
        if(nodes == null) return;
        for (MoNode moNode : nodes) {
            scan(role, moNode);
        }
    }

    public void scan(String role, MoNode moNode) {
        scan(moNode);
    }

    public void scan(MoNode element) {
        if (element != null) {
            element.accept(this);
        }
    }

    public void scan(String SimpleRole) {
        // scan child in form
    }

    @Override
    public void visitMoAnonymousClassDeclaration(MoAnonymousClassDeclaration moAnonymousClassDeclaration) {
        enter(moAnonymousClassDeclaration);
        scan("bodyDeclarations", moAnonymousClassDeclaration.getBodyDeclarations());
        exit(moAnonymousClassDeclaration);
    }

    @Override
    public void visitMoArrayAccess(MoArrayAccess moArrayAccess) {
        enter(moArrayAccess);
        scan("array", moArrayAccess.getArray());
        scan("index", moArrayAccess.getIndex());
        exit(moArrayAccess);
    }

    @Override
    public void visitMoArrayCreation(MoArrayCreation moArrayCreation) {
        enter(moArrayCreation);
        scan("type", moArrayCreation.getType());
        scan("dimensions", moArrayCreation.getDimensionExpressions());
        moArrayCreation.getInitializer().ifPresent(initializer -> scan("initializer", initializer));
        exit(moArrayCreation);
    }

    @Override
    public void visitMoArrayInitializer(MoArrayInitializer moArrayInitializer) {
        enter(moArrayInitializer);
        scan("expressions", moArrayInitializer.getExpressions());
        exit(moArrayInitializer);
    }

    @Override
    public void visitMoArrayType(MoArrayType moArrayType) {
        enter(moArrayType);
        scan("elementType", moArrayType.getElementType());
        scan("dimensions", moArrayType.getDimensions());
        exit(moArrayType);
    }

    @Override
    public void visitMoAssertStatement(MoAssertStatement moAssertStatement) {
        enter(moAssertStatement);
        scan("expression", moAssertStatement.getExpression());
        moAssertStatement.getMessage().ifPresent(message -> scan("message", message));
        exit(moAssertStatement);
    }

    @Override
    public void visitMoAssignment(MoAssignment moAssignment) {
        enter(moAssignment);
        scan("leftHandSide", moAssignment.getLeft());
        scan("operator", moAssignment.getOperator());
        scan("rightHandSide", moAssignment.getRight());
        exit(moAssignment);
    }

    @Override
    public void visitMoBlock(MoBlock moBlock) {
        enter(moBlock);
        scan("statements", moBlock.getStatements());
        exit(moBlock);
    }

    @Override
    public void visitMoBooleanLiteral(MoBooleanLiteral moBooleanLiteral) {
        enter(moBooleanLiteral);
        scan("booleanValue");
        exit(moBooleanLiteral);
    }

    @Override
    public void visitMoBreakStatement(MoBreakStatement moBreakStatement) {
        enter(moBreakStatement);
        moBreakStatement.getBreakLabel().ifPresent(label -> scan("label", label));
        exit(moBreakStatement);
    }

    @Override
    public void visitMoCastExpression(MoCastExpression moCastExpression) {
        enter(moCastExpression);
        scan("type", moCastExpression.getCastType());
        scan("expression", moCastExpression.getExpression());
        exit(moCastExpression);
    }

    @Override
    public void visitMoCatchClause(MoCatchClause moCatchClause) {
        enter(moCatchClause);
        scan("exception", moCatchClause.getException());
        scan("body", moCatchClause.getBody());
        exit(moCatchClause);
    }

    @Override
    public void visitMoCharacterLiteral(MoCharacterLiteral moCharacterLiteral) {
        enter(moCharacterLiteral);
        scan("escapedValue");
        exit(moCharacterLiteral);
    }

    @Override
    public void visitMoClassInstanceCreation(MoClassInstanceCreation moClassInstanceCreation) {
        enter(moClassInstanceCreation);
        scan("typeArguments", moClassInstanceCreation.getTypeArguments());
        scan("type", moClassInstanceCreation.getType());
        moClassInstanceCreation.getExpression().ifPresent(expression -> scan("expression", expression));
        scan("arguments", moClassInstanceCreation.getArguments());
        moClassInstanceCreation.getAnonymousClassDeclaration().ifPresent(anonymousClassDeclaration -> {
            scan("anonymousClassDeclaration", anonymousClassDeclaration);
        });
        exit(moClassInstanceCreation);
    }

    @Override
    public void visitMoCompilationUnit(MoCompilationUnit moCompilationUnit) {
        enter(moCompilationUnit);
        moCompilationUnit.getPackageDeclaration().ifPresent(packageDeclaration -> scan("package", packageDeclaration));
        scan("imports", moCompilationUnit.getImports());
        scan("types", moCompilationUnit.getTypes());
        exit(moCompilationUnit);
    }

    @Override
    public void visitMoConditionalExpression(MoConditionalExpression moConditionalExpression) {
        enter(moConditionalExpression);
        scan("expression", moConditionalExpression.getCondition());
        scan("thenExpression", moConditionalExpression.getThenExpression());
        scan("elseExpression", moConditionalExpression.getElseExpression());
        exit(moConditionalExpression);
    }

    @Override
    public void visitMoConstructorInvocation(MoConstructorInvocation moConstructorInvocation) {
        enter(moConstructorInvocation);
        scan("typeArguments", moConstructorInvocation.getArguments());
        scan("arguments", moConstructorInvocation.getArguments());
        exit(moConstructorInvocation);
    }

    @Override
    public void visitMoContinueStatement(MoContinueStatement moContinueStatement) {
        enter(moContinueStatement);
        moContinueStatement.getContinueLabel().ifPresent(label -> scan("label", label));
        exit(moContinueStatement);
    }

    @Override
    public void visitMoDoStatement(MoDoStatement moDoStatement) {
        enter(moDoStatement);
        scan("body", moDoStatement.getBody());
        scan("expression", moDoStatement.getExpression());
        exit(moDoStatement);
    }

    @Override
    public void visitMoEmptyStatement(MoEmptyStatement moEmptyStatement) {
        enter(moEmptyStatement);
        // do nothing
        exit(moEmptyStatement);
    }

    @Override
    public void visitMoExpressionStatement(MoExpressionStatement moExpressionStatement) {
        enter(moExpressionStatement);
        scan("expression", moExpressionStatement.getExpression());
        exit(moExpressionStatement);
    }

    @Override
    public void visitMoFieldAccess(MoFieldAccess moFieldAccess) {
        enter(moFieldAccess);
        scan("expression", moFieldAccess.getExpression());
        scan("name", moFieldAccess.getName());
        exit(moFieldAccess);
    }

    @Override
    public void visitMoFieldDeclaration(MoFieldDeclaration moFieldDeclaration) {
        enter(moFieldDeclaration);
        moFieldDeclaration.getJavadoc().ifPresent(javadoc -> scan("javadoc", javadoc));
        scanExtendedModifier(moFieldDeclaration.getModifiers());
        scan("type", moFieldDeclaration.getType());
        scan("fragments", moFieldDeclaration.getFragments());
        exit(moFieldDeclaration);
    }

    @Override
    public void visitMoForStatement(MoForStatement moForStatement) {
        enter(moForStatement);
        scan("initializers", moForStatement.getInitializers());
        moForStatement.getCondition().ifPresent(condition -> scan("expression", condition));
        scan("updaters", moForStatement.getUpdaters());
        scan("body", moForStatement.getBody());
        exit(moForStatement);
    }

    @Override
    public void visitMoIfStatement(MoIfStatement moIfStatement) {
        enter(moIfStatement);
        scan("expression", moIfStatement.getCondition());
        scan("thenStatement", moIfStatement.getThenStatement());
        moIfStatement.getElseStatement().ifPresent(elseStatement -> scan("elseStatement", elseStatement));
        exit(moIfStatement);
    }

    @Override
    public void visitMoImportDeclaration(MoImportDeclaration moImportDeclaration) {
        enter(moImportDeclaration);
        scan("name", moImportDeclaration.getName());
        scan("static");
        scan("onDemand");
        exit(moImportDeclaration);
    }

    @Override
    public void visitMoInfixExpression(MoInfixExpression moInfixExpression) {
        enter(moInfixExpression);
        scan("leftOperand", moInfixExpression.getLeft());
        scan("operator", moInfixExpression.getOperator());
        scan("rightOperand", moInfixExpression.getRight());
        scan("extendedOperands", moInfixExpression.getExtendedOperands());
        exit(moInfixExpression);
    }

    @Override
    public void visitMoInitializer(MoInitializer moInitializer) {
        enter(moInitializer);
        moInitializer.getJavadoc().ifPresent(javadoc -> scan("javadoc", javadoc));
        scanExtendedModifier(moInitializer.getModifiers());
        scan("body", moInitializer.getBody());
        exit(moInitializer);
    }

    @Override
    public void visitMoJavadoc(MoJavadoc moJavadoc) {
        enter(moJavadoc);
        scan("tags", moJavadoc.getTagElements());
        exit(moJavadoc);
    }

    @Override
    public void visitMoLabeledStatement(MoLabeledStatement moLabeledStatement) {
        enter(moLabeledStatement);
        scan("label", moLabeledStatement.getLabel());
        scan("body", moLabeledStatement.getStatement());
        exit(moLabeledStatement);
    }

    @Override
    public void visitMoMethodDeclaration(MoMethodDeclaration moMethodDeclaration) {
        enter(moMethodDeclaration);
        moMethodDeclaration.getJavadoc().ifPresent(javadoc -> scan("javadoc", javadoc));
        scanExtendedModifier(moMethodDeclaration.getModifiers());
        scan("name", moMethodDeclaration.getName());
        moMethodDeclaration.getReturnType().ifPresent(returnType -> scan("returnType2", returnType));
        scan("typeParameters", moMethodDeclaration.getTypeParameters());
        scan("parameters", moMethodDeclaration.getParameters());
        scan("thrownExceptionTypes", moMethodDeclaration.getThrownExceptionTypes());
        moMethodDeclaration.getBody().ifPresent(body -> scan("body", body));
        exit(moMethodDeclaration);
    }

    @Override
    public void visitMoMethodInvocation(MoMethodInvocation moMethodInvocation) {
        enter(moMethodInvocation);
        moMethodInvocation.getTarget().ifPresent(target -> scan("target", target));
        scan("typeArguments", moMethodInvocation.getTypeArguments());
        scan("name", moMethodInvocation.getName());
        scan("arguments", moMethodInvocation.getArguments());
        exit(moMethodInvocation);
    }

    @Override
    public void visitMoNullLiteral(MoNullLiteral moNullLiteral) {
        enter(moNullLiteral);
        scan("nullValue");
        exit(moNullLiteral);
    }

    @Override
    public void visitMoNumberLiteral(MoNumberLiteral moNumberLiteral) {
        enter(moNumberLiteral);
        scan("token");
        exit(moNumberLiteral);
    }

    @Override
    public void visitMoPackageDeclaration(MoPackageDeclaration moPackageDeclaration) {
        enter(moPackageDeclaration);
        moPackageDeclaration.getJavadoc().ifPresent(javadoc -> scan("javadoc", javadoc));
        scanExtendedModifier(moPackageDeclaration.getAnnotations());
        scan("name", moPackageDeclaration.getName());
        exit(moPackageDeclaration);
    }

    @Override
    public void visitMoParenthesizedExpression(MoParenthesizedExpression moParenthesizedExpression) {
        enter(moParenthesizedExpression);
        scan("expression", moParenthesizedExpression.getExpression());
        exit(moParenthesizedExpression);
    }

    @Override
    public void visitMoPostfixExpression(MoPostfixExpression moPostfixExpression) {
        enter(moPostfixExpression);
        scan("operand", moPostfixExpression.getOperand());
        scan("operator", moPostfixExpression.getOperator());
        exit(moPostfixExpression);
    }

    @Override
    public void visitMoPrefixExpression(MoPrefixExpression moPrefixExpression) {
        enter(moPrefixExpression);
        scan("operator", moPrefixExpression.getOperator());
        scan("operand", moPrefixExpression.getOperand());
        exit(moPrefixExpression);
    }

    @Override
    public void visitMoPrimitiveType(MoPrimitiveType moPrimitiveType) {
        enter(moPrimitiveType);
        scan("primitiveTypeCode");
        exit(moPrimitiveType);
    }

    @Override
    public void visitMoQualifiedName(MoQualifiedName moQualifiedName) {
        enter(moQualifiedName);
        scan("qualifier", moQualifiedName.getQualifier());
        scan("name", moQualifiedName.getName());
        exit(moQualifiedName);
    }

    @Override
    public void visitMoReturnStatement(MoReturnStatement moReturnStatement) {
        enter(moReturnStatement);
        moReturnStatement.getExpression().ifPresent(expression -> scan("expression", expression));
        exit(moReturnStatement);
    }

    @Override
    public void visitMoSimpleName(MoSimpleName moSimpleName) {
        enter(moSimpleName);
        scan("identifier");
        exit(moSimpleName);
    }

    @Override
    public void visitMoSimpleType(MoSimpleType moSimpleType) {
        enter(moSimpleType);
        scan("name", moSimpleType.getName());
        exit(moSimpleType);
    }

    @Override
    public void visitMoSingleVariableDeclaration(MoSingleVariableDeclaration moSingleVariableDeclaration) {
        enter(moSingleVariableDeclaration);
        scan("name", moSingleVariableDeclaration.getName());
        scan("extraDimensions2", moSingleVariableDeclaration.getCStyleArrayDimensions());
        moSingleVariableDeclaration.getInitializer().ifPresent(initializer -> scan("initializer", initializer));
        scanExtendedModifier(moSingleVariableDeclaration.getModifiers());
        scan("type", moSingleVariableDeclaration.getType());
        scan("varargsAnnotations", moSingleVariableDeclaration.getVarargsAnnotations());
        scan("varargs");
        moSingleVariableDeclaration.getInitializer().ifPresent(initializer -> scan("initializer", initializer));
        exit(moSingleVariableDeclaration);
    }

    @Override
    public void visitMoStringLiteral(MoStringLiteral moStringLiteral) {
        enter(moStringLiteral);
        scan("escapedValue");
        exit(moStringLiteral);
    }

    @Override
    public void visitMoSuperConstructorInvocation(MoSuperConstructorInvocation moSuperConstructorInvocation) {
        enter(moSuperConstructorInvocation);
        moSuperConstructorInvocation.getExpression().ifPresent(expression -> scan("expression", expression));
        scan("typeArguments", moSuperConstructorInvocation.getTypeArguments());
        scan("arguments", moSuperConstructorInvocation.getArguments());
        exit(moSuperConstructorInvocation);
    }

    @Override
    public void visitMoSuperFieldAccess(MoSuperFieldAccess moSuperFieldAccess) {
        enter(moSuperFieldAccess);
        moSuperFieldAccess.getQualifier().ifPresent(qualifier -> scan("qualifier", qualifier));
        scan("name", moSuperFieldAccess.getName());
        exit(moSuperFieldAccess);
    }

    @Override
    public void visitMoSuperMethodInvocation(MoSuperMethodInvocation moSuperMethodInvocation) {
        enter(moSuperMethodInvocation);
        moSuperMethodInvocation.getQualifier().ifPresent(qualifier -> scan("qualifier", qualifier));
        scan("typeArguments", moSuperMethodInvocation.getTypeArguments());
        scan("name", moSuperMethodInvocation.getName());
        scan("arguments", moSuperMethodInvocation.getArguments());
        exit(moSuperMethodInvocation);
    }

    @Override
    public void visitMoSwitchCase(MoSwitchCase moSwitchCase) {
        enter(moSwitchCase);
        moSwitchCase.getExpression().ifPresent(expression -> scan("expression", expression));
        exit(moSwitchCase);
    }

    @Override
    public void visitMoSwitchStatement(MoSwitchStatement moSwitchStatement) {
        enter(moSwitchStatement);
        scan("expression", moSwitchStatement.getExpression());
        scan("statements", moSwitchStatement.getStatements());
        exit(moSwitchStatement);
    }

    @Override
    public void visitMoSynchronizedStatement(MoSynchronizedStatement moSynchronizedStatement) {
        enter(moSynchronizedStatement);
        scan("expression", moSynchronizedStatement.getExpression());
        scan("body", moSynchronizedStatement.getBlock());
        exit(moSynchronizedStatement);
    }

    @Override
    public void visitMoThisExpression(MoThisExpression moThisExpression) {
        enter(moThisExpression);
        moThisExpression.getQualifier().ifPresent(qualifier -> scan("qualifier", qualifier));
        exit(moThisExpression);
    }

    @Override
    public void visitMoThrowStatement(MoThrowStatement moThrowStatement) {
        enter(moThrowStatement);
        scan("expression", moThrowStatement.getExpression());
        exit(moThrowStatement);
    }

    @Override
    public void visitMoTryStatement(MoTryStatement moTryStatement) {
        enter(moTryStatement);
        scan("resources", moTryStatement.getResources());
        scan("body", moTryStatement.getTryBlock());
        scan("catchClauses", moTryStatement.getCatchClauses());
        moTryStatement.getFinallyBlock().ifPresent(finallyBlock -> scan("finally", finallyBlock));
        exit(moTryStatement);
    }

    @Override
    public void visitMoTypeDeclaration(MoTypeDeclaration moTypeDeclaration) {
        enter(moTypeDeclaration);
        moTypeDeclaration.getJavadoc().ifPresent(javadoc -> scan("javadoc", javadoc));
        scanExtendedModifier(moTypeDeclaration.getModifiers());
        scan("name", moTypeDeclaration.getName());
        scan("bodyDeclarations", moTypeDeclaration.getBodyDeclarations());

        moTypeDeclaration.getSuperclassType().ifPresent(superclassType -> scan("superclassType", superclassType));
        scan("superInterfaceTypes", moTypeDeclaration.getSuperInterfaceTypes());
        scan("typeParameters", moTypeDeclaration.getTypeParameters());
        exit(moTypeDeclaration);
    }

    @Override
    public void visitMoTypeDeclarationStatement(MoTypeDeclarationStatement moTypeDeclarationStatement) {
        enter(moTypeDeclarationStatement);
        scan("declaration", moTypeDeclarationStatement.getTypeDeclaration());
        exit(moTypeDeclarationStatement);
    }

    @Override
    public void visitMoTypeLiteral(MoTypeLiteral moTypeLiteral) {
        enter(moTypeLiteral);
        scan("type", moTypeLiteral.getType());
        exit(moTypeLiteral);
    }

    @Override
    public void visitMoVariableDeclarationExpression(MoVariableDeclarationExpression moVariableDeclarationExpression) {
        enter(moVariableDeclarationExpression);
        scanExtendedModifier(moVariableDeclarationExpression.getModifiers());
        scan("type", moVariableDeclarationExpression.getType());
        scan("fragments", moVariableDeclarationExpression.getFragments());
        exit(moVariableDeclarationExpression);
    }

    @Override
    public void visitMoVariableDeclarationFragment(MoVariableDeclarationFragment moVariableDeclarationFragment) {
        enter(moVariableDeclarationFragment);
        scan("name", moVariableDeclarationFragment.getName());
        scan("extraDimensions2", moVariableDeclarationFragment.getCStyleArrayDimensions());
        moVariableDeclarationFragment.getInitializer().ifPresent(initializer -> scan("initializer", initializer));
        exit(moVariableDeclarationFragment);
    }

    @Override
    public void visitMoVariableDeclarationStatement(MoVariableDeclarationStatement moVariableDeclarationStatement) {
        enter(moVariableDeclarationStatement);
        scanExtendedModifier(moVariableDeclarationStatement.getModifiers());
        scan("type", moVariableDeclarationStatement.getType());
        scan("fragments", moVariableDeclarationStatement.getFragments());
        exit(moVariableDeclarationStatement);
    }

    @Override
    public void visitMoWhileStatement(MoWhileStatement moWhileStatement) {
        enter(moWhileStatement);
        scan("expression", moWhileStatement.getCondition());
        scan("body", moWhileStatement.getBody());
        exit(moWhileStatement);
    }

    @Override
    public void visitMoInstanceofExpression(MoInstanceofExpression moInstanceofExpression) {
        enter(moInstanceofExpression);
        scan("leftOperand", moInstanceofExpression.getLeftOperand());
        scan("rightOperand", moInstanceofExpression.getRightOperand());
        exit(moInstanceofExpression);
    }

    @Override
    public void visitMoLineComment(MoLineComment moLineComment) {
        enter(moLineComment);
        // do nothing
        exit(moLineComment);
    }

    @Override
    public void visitMoBlockComment(MoBlockComment moBlockComment) {
        enter(moBlockComment);
        // do nothing
        exit(moBlockComment);
    }

    @Override
    public void visitMoTagElement(MoTagElement moTagElement) {
        enter(moTagElement);
        scan("tagName");
        scanDocElement(moTagElement.getDocFragments());
        exit(moTagElement);
    }

    @Override
    public void visitMoTextElement(MoTextElement moTextElement) {
        enter(moTextElement);
        scan("text");
        exit(moTextElement);
    }

    @Override
    public void visitMoEnhancedForStatement(MoEnhancedForStatement moEnhancedForStatement) {
        enter(moEnhancedForStatement);
        scan("parameter", moEnhancedForStatement.getParameter());
        scan("expression", moEnhancedForStatement.getExpression());
        scan("body", moEnhancedForStatement.getBody());
        exit(moEnhancedForStatement);
    }

    @Override
    public void visitMoEnumDeclaration(MoEnumDeclaration moEnumDeclaration) {
        enter(moEnumDeclaration);
        moEnumDeclaration.getJavadoc().ifPresent(javadoc -> scan("javadoc", javadoc));
        scanExtendedModifier(moEnumDeclaration.getModifiers());
        scan("name", moEnumDeclaration.getName());
        scan("superInterfaceTypes", moEnumDeclaration.getSuperInterfaceTypes());
        scan("enumConstants", moEnumDeclaration.getEnumConstants());
        scan("bodyDeclarations", moEnumDeclaration.getBodyDeclarations());
        exit(moEnumDeclaration);
    }

    @Override
    public void visitMoEnumConstantDeclaration(MoEnumConstantDeclaration moEnumConstantDeclaration) {
        enter(moEnumConstantDeclaration);
        moEnumConstantDeclaration.getJavadoc().ifPresent(javadoc -> scan("javadoc", javadoc));
        scanExtendedModifier(moEnumConstantDeclaration.getModifiers());
        scan("name", moEnumConstantDeclaration.getName());
        scan("arguments", moEnumConstantDeclaration.getArguments());
        moEnumConstantDeclaration.getAnonymousClassDeclaration().ifPresent(anonymousClassDeclaration -> {
            scan("anonymousClassDeclaration", anonymousClassDeclaration);
        });
        exit(moEnumConstantDeclaration);
    }

    @Override
    public void visitMoTypeParameter(MoTypeParameter moTypeParameter) {
        enter(moTypeParameter);
        scanExtendedModifier(moTypeParameter.getModifiers());
        scan("name", moTypeParameter.getName());
        scan("typeBounds", moTypeParameter.getTypeBounds());
        exit(moTypeParameter);
    }

    @Override
    public void visitMoParameterizedType(MoParameterizedType moParameterizedType) {
        enter(moParameterizedType);
        scan("type", moParameterizedType.getType());
        scan("typeArguments", moParameterizedType.getTypeArguments());
        exit(moParameterizedType);
    }

    @Override
    public void visitMoQualifiedType(MoQualifiedType moQualifiedType) {
        enter(moQualifiedType);
        scanExtendedModifier(moQualifiedType.getAnnotations());
        scan("qualifier", moQualifiedType.getQualifier());
        scan("name", moQualifiedType.getSimpleName());
        exit(moQualifiedType);
    }

    @Override
    public void visitMoWildcardType(MoWildcardType moWildcardType) {
        enter(moWildcardType);
        scanExtendedModifier(moWildcardType.getAnnotations());
        moWildcardType.getBound().ifPresent(bound -> scan("bound", bound));
        exit(moWildcardType);
    }

    @Override
    public void visitMoNormalAnnotation(MoNormalAnnotation moNormalAnnotation) {
        enter(moNormalAnnotation);
        scan("typeName", moNormalAnnotation.getTypeName());
        scan("values", moNormalAnnotation.getMemberValuePairs());
        exit(moNormalAnnotation);
    }

    @Override
    public void visitMoMarkerAnnotation(MoMarkerAnnotation moMarkerAnnotation) {
        enter(moMarkerAnnotation);
        scan("typeName", moMarkerAnnotation.getTypeName());
        exit(moMarkerAnnotation);
    }

    @Override
    public void visitMoSingleMemberAnnotation(MoSingleMemberAnnotation moSingleMemberAnnotation) {
        enter(moSingleMemberAnnotation);
        scan("typeName", moSingleMemberAnnotation.getTypeName());
        scan("value", moSingleMemberAnnotation.getValue());
        exit(moSingleMemberAnnotation);
    }

    @Override
    public void visitMoMemberValuePair(MoMemberValuePair moMemberValuePair) {
        enter(moMemberValuePair);
        scan("name", moMemberValuePair.getName());
        scan("value", moMemberValuePair.getValue());
        exit(moMemberValuePair);
    }

    @Override
    public void visitMoModifier(MoModifier moModifier) {
        enter(moModifier);
        scan("modifierKind");
        exit(moModifier);
    }

    @Override
    public void visitMoUnionType(MoUnionType moUnionType) {
        enter(moUnionType);
        scan("types", moUnionType.getTypes());
        exit(moUnionType);
    }

    @Override
    public void visitMoDimension(MoDimension moDimension) {
        enter(moDimension);
        scan("annotations", moDimension.getAnnotations());
        exit(moDimension);
    }

    @Override
    public void visitMoLambdaExpression(MoLambdaExpression moLambdaExpression) {
        enter(moLambdaExpression);
        scan("parameters", moLambdaExpression.getParameters());
        scan("body", moLambdaExpression.getBody());
        exit(moLambdaExpression);
    }

    @Override
    public void visitMoIntersectionType(MoIntersectionType moIntersectionType) {
        enter(moIntersectionType);
        scan("types", moIntersectionType.getTypes());
        exit(moIntersectionType);
    }

    @Override
    public void visitMoNameQualifiedType(MoNameQualifiedType moNameQualifiedType) {
        enter(moNameQualifiedType);
        scanExtendedModifier(moNameQualifiedType.getAnnotations());
        scan("qualifier", moNameQualifiedType.getQualifier());
        scan("name", moNameQualifiedType.getSimpleName());
        exit(moNameQualifiedType);
    }

    @Override
    public void visitMoCreationReference(MoCreationReference moCreationReference) {
        enter(moCreationReference);
        scan("typeArguments", moCreationReference.getTypeArguments());
        scan("type", moCreationReference.getType());
        exit(moCreationReference);
    }

    @Override
    public void visitMoExpressionMethodReference(MoExpressionMethodReference moExpressionMethodReference) {
        enter(moExpressionMethodReference);
        scan("expression", moExpressionMethodReference.getExpression());
        scan("typeArguments", moExpressionMethodReference.getTypeArguments());
        scan("name", moExpressionMethodReference.getSimpleName());
        exit(moExpressionMethodReference);
    }

    @Override
    public void visitMoSuperMethodReference(MoSuperMethodReference moSuperMethodReference) {
        enter(moSuperMethodReference);
        moSuperMethodReference.getQualifier().ifPresent(qualifier -> scan("qualifier", qualifier));
        scan("typeArguments", moSuperMethodReference.getTypeArguments());
        scan("name", moSuperMethodReference.getSimpleName());
        exit(moSuperMethodReference);
    }

    @Override
    public void visitMoTypeMethodReference(MoTypeMethodReference moTypeMethodReference) {
        enter(moTypeMethodReference);
        scan("type", moTypeMethodReference.getType());
        scan("typeArguments", moTypeMethodReference.getTypeArguments());
        scan("name", moTypeMethodReference.getSimpleName());
        exit(moTypeMethodReference);
    }

    @Override
    public void visitMoInfixOperator(MoInfixOperator moInfixOperator) {
        enter(moInfixOperator);
        scan("operator");
        exit(moInfixOperator);
    }

    @Override
    public void visitMoAssignmentOperator(MoAssignmentOperator moAssignmentOperator) {
        enter(moAssignmentOperator);
        scan("operator");
        exit(moAssignmentOperator);
    }

    @Override
    public void visitMoPostfixOperator(MoPostfixOperator moPostfixOperator) {
        enter(moPostfixOperator);
        scan("operator");
        exit(moPostfixOperator);
    }

    @Override
    public void visitMoPrefixOperator(MoPrefixOperator moPrefixOperator) {
        enter(moPrefixOperator);
        scan("operator");
        exit(moPrefixOperator);
    }

    @Override
    public void visitMoMethodInvocationTarget(MoMethodInvocationTarget moMethodInvocationTarget) {
        enter(moMethodInvocationTarget);
        scan("expression", moMethodInvocationTarget.getExpression());
        exit(moMethodInvocationTarget);
    }

    @Override
    public void visitMoMethodInvocationArguments(MoMethodInvocationArguments moMethodInvocationArguments) {
        enter(moMethodInvocationArguments);
        scan("arguments", moMethodInvocationArguments.getArguments());
        exit(moMethodInvocationArguments);
    }

    private void scanExtendedModifier(List<? extends MoExtendedModifier> extendedModifiers) {
        for (MoExtendedModifier moExtendedModifier : extendedModifiers) {
            if (moExtendedModifier instanceof MoAnnotation annotation) {
                scan("annotation", annotation);
            } else if (moExtendedModifier instanceof MoModifier modifier) {
                scan("modifier", modifier);
            }
        }
    }

    private void scanDocElement(List<? extends MoDocElement> docElements) {
        for (MoDocElement docElement : docElements) {
            if (docElement instanceof MoTagElement tagElement) {
                scan("tagElement", tagElement);
            } else if (docElement instanceof MoTextElement textElement) {
                scan("modifier", textElement);
            } else if (docElement instanceof MoName name) {
                scan("name", name);
            }
        }
    }

}

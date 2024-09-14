package repair.ast.visitor;

import repair.ast.MoCompilationUnit;
import repair.ast.code.*;
import repair.ast.code.expression.*;
import repair.ast.code.expression.literal.*;
import repair.ast.code.statement.*;
import repair.ast.code.type.*;
import repair.ast.code.virtual.*;
import repair.ast.declaration.*;

public interface Visitor {
    void visitMoAnonymousClassDeclaration(MoAnonymousClassDeclaration moAnonymousClassDeclaration); // 1
    void visitMoArrayAccess(MoArrayAccess moArrayAccess); // 2
    void visitMoArrayCreation(MoArrayCreation moArrayCreation); // 3
    void visitMoArrayInitializer(MoArrayInitializer moArrayInitializer); // 4
    void visitMoArrayType(MoArrayType moArrayType); // 5
    void visitMoAssertStatement(MoAssertStatement moAssertStatement); // 6
    void visitMoAssignment(MoAssignment moAssignment); // 7
    void visitMoBlock(MoBlock moBlock); // 8
    void visitMoBooleanLiteral(MoBooleanLiteral moBooleanLiteral); // 9
    void visitMoBreakStatement(MoBreakStatement moBreakStatement); // 10
    void visitMoCastExpression(MoCastExpression moCastExpression); // 11
    void visitMoCatchClause(MoCatchClause moCatchClause); // 12
    void visitMoCharacterLiteral(MoCharacterLiteral moCharacterLiteral); // 13
    void visitMoClassInstanceCreation(MoClassInstanceCreation moClassInstanceCreation); // 14
    void visitMoCompilationUnit(MoCompilationUnit moCompilationUnit); // 15
    void visitMoConditionalExpression(MoConditionalExpression moConditionalExpression); // 16
    void visitMoConstructorInvocation(MoConstructorInvocation moConstructorInvocation); // 17
    void visitMoContinueStatement(MoContinueStatement moContinueStatement); // 18
    void visitMoDoStatement(MoDoStatement moDoStatement); // 19
    void visitMoEmptyStatement(MoEmptyStatement moEmptyStatement); // 20
    void visitMoExpressionStatement(MoExpressionStatement moExpressionStatement); // 21
    void visitMoFieldAccess(MoFieldAccess moFieldAccess); // 22
    void visitMoFieldDeclaration(MoFieldDeclaration moFieldDeclaration); // 23
    void visitMoForStatement(MoForStatement moForStatement); // 24
    void visitMoIfStatement(MoIfStatement moIfStatement); // 25
    void visitMoImportDeclaration(MoImportDeclaration moImportDeclaration); // 26
    void visitMoInfixExpression(MoInfixExpression moInfixExpression); // 27
    void visitMoInitializer(MoInitializer moInitializer); // 28
    void visitMoJavadoc(MoJavadoc moJavadoc); // 29
    void visitMoLabeledStatement(MoLabeledStatement moLabeledStatement); //30
    void visitMoMethodDeclaration(MoMethodDeclaration moMethodDeclaration); // 31
    void visitMoMethodInvocation(MoMethodInvocation moMethodInvocation); // 32
    void visitMoNullLiteral(MoNullLiteral moNullLiteral); // 33
    void visitMoNumberLiteral(MoNumberLiteral moNumberLiteral); // 34
    void visitMoPackageDeclaration(MoPackageDeclaration moPackageDeclaration); // 35
    void visitMoParenthesizedExpression(MoParenthesizedExpression moParenthesizedExpression); // 36
    void visitMoPostfixExpression(MoPostfixExpression moPostfixExpression); // 37
    void visitMoPrefixExpression(MoPrefixExpression moPrefixExpression); // 38
    void visitMoPrimitiveType(MoPrimitiveType moPrimitiveType); // 39
    void visitMoQualifiedName(MoQualifiedName moQualifiedName); // 40
    void visitMoReturnStatement(MoReturnStatement moReturnStatement); // 41
    void visitMoSimpleName(MoSimpleName moSimpleName); // 42
    void visitMoSimpleType(MoSimpleType moSimpleType); // 43
    void visitMoSingleVariableDeclaration(MoSingleVariableDeclaration moSingleVariableDeclaration); // 44
    void visitMoStringLiteral(MoStringLiteral moStringLiteral); // 45
    void visitMoSuperConstructorInvocation(MoSuperConstructorInvocation moSuperConstructorInvocation); // 46
    void visitMoSuperFieldAccess(MoSuperFieldAccess moSuperFieldAccess); // 47
    void visitMoSuperMethodInvocation(MoSuperMethodInvocation moSuperMethodInvocation); // 48
    void visitMoSwitchCase(MoSwitchCase moSwitchCase); // 49
    void visitMoSwitchStatement(MoSwitchStatement moSwitchStatement); // 50
    void visitMoSynchronizedStatement(MoSynchronizedStatement moSynchronizedStatement); // 51
    void visitMoThisExpression(MoThisExpression moThisExpression); // 52
    void visitMoThrowStatement(MoThrowStatement moThrowStatement); // 53
    void visitMoTryStatement(MoTryStatement moTryStatement); // 54
    void visitMoTypeDeclaration(MoTypeDeclaration moTypeDeclaration); // 55
    void visitMoTypeDeclarationStatement(MoTypeDeclarationStatement moTypeDeclarationStatement); // 56
    void visitMoTypeLiteral(MoTypeLiteral moTypeLiteral); // 57
    void visitMoVariableDeclarationExpression(MoVariableDeclarationExpression moVariableDeclarationExpression); // 58
    void visitMoVariableDeclarationFragment(MoVariableDeclarationFragment moVariableDeclarationFragment); // 59
    void visitMoVariableDeclarationStatement(MoVariableDeclarationStatement moVariableDeclarationStatement); // 60
    void visitMoWhileStatement(MoWhileStatement moWhileStatement); // 61
    void visitMoInstanceofExpression(MoInstanceofExpression moInstanceofExpression); // 62
    void visitMoLineComment(MoLineComment moLineComment); // 63
    void visitMoBlockComment(MoBlockComment moBlockComment); // 64
    void visitMoTagElement(MoTagElement moTagElement); // 65
    void visitMoTextElement(MoTextElement moTextElement); // 66
    void visitMoEnhancedForStatement(MoEnhancedForStatement moEnhancedForStatement); // 70
    void visitMoEnumDeclaration(MoEnumDeclaration moEnumDeclaration); // 71
    void visitMoEnumConstantDeclaration(MoEnumConstantDeclaration moEnumConstantDeclaration); // 72
    void visitMoTypeParameter(MoTypeParameter moTypeParameter); // 73
    void visitMoParameterizedType(MoParameterizedType moParameterizedType); // 74
    void visitMoQualifiedType(MoQualifiedType moQualifiedType); // 75
    void visitMoWildcardType(MoWildcardType moWildcardType); // 76
    void visitMoNormalAnnotation(MoNormalAnnotation moNormalAnnotation); // 77
    void visitMoMarkerAnnotation(MoMarkerAnnotation moMarkerAnnotation); // 78
    void visitMoSingleMemberAnnotation(MoSingleMemberAnnotation moSingleMemberAnnotation); // 79
    void visitMoMemberValuePair(MoMemberValuePair moMemberValuePair); // 80
    void visitMoModifier(MoModifier moModifier); // 83
    void visitMoUnionType(MoUnionType moUnionType); // 84
    void visitMoDimension(MoDimension moDimension); // 85
    void visitMoLambdaExpression(MoLambdaExpression moLambdaExpression); // 86
    void visitMoIntersectionType(MoIntersectionType moIntersectionType); // 87
    void visitMoNameQualifiedType(MoNameQualifiedType moNameQualifiedType); // 88
    void visitMoCreationReference(MoCreationReference moCreationReference); // 89
    void visitMoExpressionMethodReference(MoExpressionMethodReference moExpressionMethodReference); // 90
    void visitMoSuperMethodReference(MoSuperMethodReference moSuperMethodReference); // 91
    void visitMoTypeMethodReference(MoTypeMethodReference moTypeMethodReference); // 92

    // Virtual
    void visitMoInfixOperator(MoInfixOperator moInfixOperator);
    void visitMoAssignmentOperator(MoAssignmentOperator moAssignmentOperator);
    void visitMoPostfixOperator(MoPostfixOperator moPostfixOperator);
    void visitMoPrefixOperator(MoPrefixOperator moPrefixOperator);

    void visitMoMethodInvocationTarget(MoMethodInvocationTarget moMethodInvocationTarget);
    void visitMoMethodInvocationArguments(MoMethodInvocationArguments moMethodInvocationArguments);

}

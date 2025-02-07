package repair.dsl.kirin.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import repair.ast.code.*;
import repair.ast.code.expression.*;
import repair.ast.code.expression.literal.*;
import repair.ast.code.statement.*;
import repair.ast.code.type.*;
import repair.ast.code.virtual.*;
import repair.ast.declaration.*;
import repair.ast.role.Description;
import repair.dsl.kirin.map.code.role.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

public class DSLRoleMapping {
    private static final Logger logger = LoggerFactory.getLogger(DSLRoleMapping.class);

    private static final Map<Description<? extends MoNode, ?>, Class<? extends DSLRole>> roleMapping = Map.<Description<? extends MoNode, ?>, Class<? extends DSLRole>>ofEntries(
            entry(Objects.requireNonNull(findStaticDescription(MoModifier.class, "keywordDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeParameter.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeParameter.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeParameter.class, "typeBoundsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPackageDeclaration.class, "javadocDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPackageDeclaration.class, "annotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPackageDeclaration.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTextElement.class, "textDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoJavadoc.class, "tagsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoImportDeclaration.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoImportDeclaration.class, "staticDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoImportDeclaration.class, "onDemandDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoAnonymousClassDeclaration.class, "bodyDeclarationsDescription")), AnonymousClassBody.class),
            entry(Objects.requireNonNull(findStaticDescription(MoExpressionStatement.class, "expressionDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTryStatement.class, "resourcesDescription")), TryResources.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTryStatement.class, "bodyDescription")), TryBlock.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTryStatement.class, "catchClausesDescription")), CatchBlocks.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTryStatement.class, "finallyDescription")), FinallyBlock.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnhancedForStatement.class, "parameterDescription")), ForEachVariable.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnhancedForStatement.class, "expressionDescription")), ForEachIterable.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnhancedForStatement.class, "bodyDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSwitchStatement.class, "expressionDescription")), SwitchSelector.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSwitchStatement.class, "statementsDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclarationStatement.class, "declarationDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoDoStatement.class, "bodyDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoDoStatement.class, "expressionDescription")), DSLCondition.class),
            entry(Objects.requireNonNull(findStaticDescription(MoReturnStatement.class, "expressionDescription")), ReturnValue.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperConstructorInvocation.class, "expressionDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperConstructorInvocation.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperConstructorInvocation.class, "argumentsDescription")), Arguments.class),
            entry(Objects.requireNonNull(findStaticDescription(MoContinueStatement.class, "continueLabelDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoConstructorInvocation.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoConstructorInvocation.class, "argumentsDescription")), Arguments.class),
            entry(Objects.requireNonNull(findStaticDescription(MoWhileStatement.class, "expressionDescription")), DSLCondition.class),
            entry(Objects.requireNonNull(findStaticDescription(MoWhileStatement.class, "bodyDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoIfStatement.class, "expressionDescription")), DSLCondition.class),
            entry(Objects.requireNonNull(findStaticDescription(MoIfStatement.class, "thenStatementDescription")), ThenBlock.class),
            entry(Objects.requireNonNull(findStaticDescription(MoIfStatement.class, "elseStatementDescription")), ElseBlock.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationStatement.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationStatement.class, "typeDescription")), DSLType.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationStatement.class, "fragmentsDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoBreakStatement.class, "breakLabelDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoAssertStatement.class, "expressionDescription")), DSLCondition.class),
            entry(Objects.requireNonNull(findStaticDescription(MoAssertStatement.class, "messageDescription")), AssertMessage.class),
            entry(Objects.requireNonNull(findStaticDescription(MoForStatement.class, "initializersDescription")), ForInit.class),
            entry(Objects.requireNonNull(findStaticDescription(MoForStatement.class, "expressionDescription")), DSLCondition.class),
            entry(Objects.requireNonNull(findStaticDescription(MoForStatement.class, "updatersDescription")), ForIter.class),
            entry(Objects.requireNonNull(findStaticDescription(MoForStatement.class, "bodyDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSynchronizedStatement.class, "expressionDescription")), Lock.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSynchronizedStatement.class, "bodyDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoLabeledStatement.class, "labelDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoLabeledStatement.class, "bodyDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoBlock.class, "statementsDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoThrowStatement.class, "expressionDescription")), Operand.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSwitchCase.class, "expressionDescription")), CaseExpression.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTagElement.class, "tagNameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTagElement.class, "fragmentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCatchClause.class, "exceptionDescription")), Parameters.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCatchClause.class, "bodyDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoDimension.class, "annotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMemberValuePair.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMemberValuePair.class, "valueDescription")), AnnoValue.class),


            entry(Objects.requireNonNull(findStaticDescription(MoEnumConstantDeclaration.class, "javadocDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumConstantDeclaration.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumConstantDeclaration.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumConstantDeclaration.class, "argumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumConstantDeclaration.class, "anonymousClassDeclDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumDeclaration.class, "javadocDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumDeclaration.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumDeclaration.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumDeclaration.class, "bodyDeclarationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumDeclaration.class, "superInterfaceTypesDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoEnumDeclaration.class, "enumConstantsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "javadocDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "bodyDeclarationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "interfaceDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "superclassTypeDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "superInterfaceTypesDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeDeclaration.class, "typeParametersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoFieldDeclaration.class, "javadocDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoFieldDeclaration.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoFieldDeclaration.class, "typeDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoFieldDeclaration.class, "fragmentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInitializer.class, "javadocDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInitializer.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInitializer.class, "bodyDescription")), DSLUnSupportRole.class),

            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "javadocDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "constructorDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "returnTypeDescription")), DSLType.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "typeParametersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "parametersDescription")), Parameters.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "thrownExceptionTypesDescription")), ThrowExceptionTypes.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodDeclaration.class, "bodyDescription")), Body.class),

            entry(Objects.requireNonNull(findStaticDescription(MoArrayType.class, "elementTypeDescription")), BaseType.class),
            entry(Objects.requireNonNull(findStaticDescription(MoArrayType.class, "dimensionsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSimpleType.class, "annotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSimpleType.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPrimitiveType.class, "annotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPrimitiveType.class, "primitiveTypeCodeDescription")), PrimitiveTypeCode.class),
            entry(Objects.requireNonNull(findStaticDescription(MoQualifiedType.class, "annotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoQualifiedType.class, "qualifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoQualifiedType.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoWildcardType.class, "annotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoWildcardType.class, "boundDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoWildcardType.class, "upperBoundDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoNameQualifiedType.class, "annotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoNameQualifiedType.class, "qualifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoNameQualifiedType.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoIntersectionType.class, "typesDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoUnionType.class, "typesDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoParameterizedType.class, "typeDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoParameterizedType.class, "typeArgumentsDescription")), Generics.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCompilationUnit.class, "packageDeclarationDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCompilationUnit.class, "importsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCompilationUnit.class, "typesDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationFragment.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationFragment.class, "extraDimensionsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationFragment.class, "initializerDescription")), VariableInit.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleVariableDeclaration.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleVariableDeclaration.class, "extraDimensionsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleVariableDeclaration.class, "initializerDescription")), VariableInit.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleVariableDeclaration.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleVariableDeclaration.class, "typeDescription")), DSLType.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleVariableDeclaration.class, "varargsAnnotationsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleVariableDeclaration.class, "varargsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoParenthesizedExpression.class, "expressionDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoLambdaExpression.class, "parenthesesDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoLambdaExpression.class, "parametersDescription")), Parameters.class),
            entry(Objects.requireNonNull(findStaticDescription(MoLambdaExpression.class, "bodyDescription")), Body.class),
            entry(Objects.requireNonNull(findStaticDescription(MoArrayAccess.class, "arrayDescription")), Base.class),
            entry(Objects.requireNonNull(findStaticDescription(MoArrayAccess.class, "indexDescription")), ArrayIndex.class),
            entry(Objects.requireNonNull(findStaticDescription(MoStringLiteral.class, "valueDescription")), LiteralValue.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCastExpression.class, "castTypeDescription")), CastType.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCastExpression.class, "expressionDescription")), Operand.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInfixExpression.class, "leftOperandDescription")), DSLLhs.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInfixExpression.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInfixExpression.class, "rightOperandDescription")), DSLRhs.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInfixExpression.class, "extendedOperandsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodInvocation.class, "expressionDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodInvocation.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodInvocation.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodInvocation.class, "argumentsDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperMethodInvocation.class, "qualifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperMethodInvocation.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperMethodInvocation.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperMethodInvocation.class, "argumentsDescription")), Arguments.class),
            entry(Objects.requireNonNull(findStaticDescription(MoArrayCreation.class, "typeDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoArrayCreation.class, "dimensionDescription")), Dimensions.class),
            entry(Objects.requireNonNull(findStaticDescription(MoArrayCreation.class, "initializerDescription")), InitArray.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPostfixExpression.class, "operandDescription")), Operand.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPostfixExpression.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCharacterLiteral.class, "valueDescription")), LiteralValue.class),
            entry(Objects.requireNonNull(findStaticDescription(MoBooleanLiteral.class, "booleanValueDescription")), LiteralValue.class),
            entry(Objects.requireNonNull(findStaticDescription(MoClassInstanceCreation.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoClassInstanceCreation.class, "typeDescription")), DSLType.class),
            entry(Objects.requireNonNull(findStaticDescription(MoClassInstanceCreation.class, "expressionDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoClassInstanceCreation.class, "argumentsDescription")), Arguments.class),
            entry(Objects.requireNonNull(findStaticDescription(MoClassInstanceCreation.class, "anonymousDeclDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoAssignment.class, "leftHandSideDescription")), DSLLhs.class),
            entry(Objects.requireNonNull(findStaticDescription(MoAssignment.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoAssignment.class, "rightHandSideDescription")), DSLRhs.class),
            entry(Objects.requireNonNull(findStaticDescription(MoNormalAnnotation.class, "typeNameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoNormalAnnotation.class, "valuesDescription")), AnnoValue.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMarkerAnnotation.class, "typeNameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleMemberAnnotation.class, "typeNameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSingleMemberAnnotation.class, "valueDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoNullLiteral.class, "nullDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoNumberLiteral.class, "valueDescription")), LiteralValue.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperMethodReference.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperMethodReference.class, "qualifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperMethodReference.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoExpressionMethodReference.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoExpressionMethodReference.class, "expressionDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoExpressionMethodReference.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCreationReference.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoCreationReference.class, "typeDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeMethodReference.class, "typeArgumentsDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeMethodReference.class, "typeDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeMethodReference.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoThisExpression.class, "qualifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperFieldAccess.class, "qualifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSuperFieldAccess.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoConditionalExpression.class, "conditionDescription")), DSLCondition.class),
            entry(Objects.requireNonNull(findStaticDescription(MoConditionalExpression.class, "thenExpressionDescription")), ThenExpression.class),
            entry(Objects.requireNonNull(findStaticDescription(MoConditionalExpression.class, "elseExpressionDescription")), ElseExpression.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPrefixExpression.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPrefixExpression.class, "operandDescription")), Operand.class),
            entry(Objects.requireNonNull(findStaticDescription(MoFieldAccess.class, "expressionDescription")), Base.class),
            entry(Objects.requireNonNull(findStaticDescription(MoFieldAccess.class, "nameDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoQualifiedName.class, "qualifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoQualifiedName.class, "nameDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoSimpleName.class, "identifierDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationExpression.class, "modifiersDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationExpression.class, "typeDescription")), DSLType.class),
            entry(Objects.requireNonNull(findStaticDescription(MoVariableDeclarationExpression.class, "fragmentsDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInstanceofExpression.class, "leftOperandDescription")), DSLLhs.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInstanceofExpression.class, "rightOperandDescription")), DSLRhs.class),
            entry(Objects.requireNonNull(findStaticDescription(MoArrayInitializer.class, "expressionDescription")), ArrayInitElements.class),
            entry(Objects.requireNonNull(findStaticDescription(MoTypeLiteral.class, "typeDescription")), DSLUnSupportRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPostfixOperator.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoAssignmentOperator.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodInvocationTarget.class, "expressionDescription")), Base.class),
            entry(Objects.requireNonNull(findStaticDescription(MoInfixOperator.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoPrefixOperator.class, "operatorDescription")), SkipRole.class),
            entry(Objects.requireNonNull(findStaticDescription(MoMethodInvocationArguments.class, "argumentsDescription")), Arguments.class)
    );

    public static Class<? extends DSLRole> convertRelationRole(MoNode parent, MoNode child) {
        Description<? extends MoNode, ?> description = child.getLocationInParent();

        // 针对函数定义和形参位置annotation的特殊处理
        if (parent instanceof MoAnnotation) {
            if (child instanceof MoName) {
                if ("typeName".equals(description.role())) {
                    return SkipRole.class;
                }

                if ("value".equals(description.role())) {
                    return AnnoValue.class;
                }
            }
            return AnnoMembers.class;
        }

        if (parent instanceof MoMethodDeclaration && child instanceof MoAnnotation) {
            return Annotations.class;
        }

        if (parent instanceof MoSingleVariableDeclaration && child instanceof MoAnnotation) {
            return Annotations.class;
        }

        // 针对二元表达式中多个operand的处理
        if (parent instanceof MoInfixExpression infixExpression) {
            if (! infixExpression.getExtendedOperands().isEmpty() && "operator".equals(description.role())) {
                return DSLUnSupportRole.class;
            }
        }

        // 针对forstmt中多个init和iter的特殊处理
        if (parent instanceof MoForStatement forStatement) {
            if ((forStatement.getInitializers().size() > 1 || forStatement.getUpdaters().size() > 1)
                    && ("initializers".equals(description.role()) || "updaters".equals(description.role())) ) {
                return DSLUnSupportRole.class;
            }
        }

        // 针对classInstanceCreation中泛型的特殊处理
        if (parent instanceof MoParameterizedType parameterizedType) {
            if (parameterizedType.getParent() instanceof MoClassInstanceCreation && "typeArguments".equals(description.role())) {
                return DSLUnSupportRole.class;
            }
        }

        return roleMapping.get(description);
    }

    private static Description<? extends MoNode, ?> findStaticDescription(Class<? extends MoNode> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object fieldValue = field.get(null);
            return (Description<? extends MoNode, ?>) fieldValue;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Failed to find static description", e);
        }
        return null;
    }
}

package repair.ast.visitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import repair.ast.code.*;
import repair.ast.code.expression.*;
import repair.ast.code.expression.literal.*;
import repair.ast.code.statement.*;
import repair.ast.code.type.*;
import repair.ast.code.virtual.MoAssignmentOperator;
import repair.ast.code.virtual.MoInfixOperator;
import repair.ast.code.virtual.MoPostfixOperator;
import repair.ast.code.virtual.MoPrefixOperator;
import repair.ast.declaration.*;

import java.util.ArrayList;
import java.util.List;

public class TokenizeScanner implements Visitor {
    private static final Logger logger = LoggerFactory.getLogger(TokenizeScanner.class);

    private final List<String> tokens = new ArrayList<>();

    public List<String> getTokens() {
        return camelSplit(tokens).stream().filter(token -> !token.isBlank()).toList();
    }

    public void scan(String role, MoNode moNode) {
        scan(moNode);
    }

    public void scan(MoNode element) {
        if (element != null) {
            element.accept(this);
        }
    }

    @Override
    public void visitMoAnonymousClassDeclaration(MoAnonymousClassDeclaration moAnonymousClassDeclaration) {
        tokens.add("{");
        moAnonymousClassDeclaration.getBodyDeclarations().forEach(moBodyDeclaration -> {
            scan("bodyDeclarations", moBodyDeclaration);
        });
        tokens.add("}");
    }

    @Override
    public void visitMoArrayAccess(MoArrayAccess moArrayAccess) {
        scan("array", moArrayAccess.getArray());
        tokens.add("[");
        scan("index", moArrayAccess.getIndex());
        tokens.add("]");
    }

    @Override
    public void visitMoArrayCreation(MoArrayCreation moArrayCreation) {
        tokens.add("new");
        scan("elementType", moArrayCreation.getType().getElementType());
        moArrayCreation.getDimensionExpressions().forEach(dimensionExpression -> {
            tokens.add("[");
            scan("expression", dimensionExpression);
            tokens.add("]");
        });

        int expressionSize = moArrayCreation.getDimensionExpressions().size();
        int dimensionSize = moArrayCreation.getType().getDimensions().size();

        for (int i = 0; i < dimensionSize - expressionSize; i++) {
            tokens.add("[]");
        }

        moArrayCreation.getInitializer().ifPresent(moArrayInitializer -> {
            scan("initializer", moArrayInitializer);
        });
    }

    @Override
    public void visitMoArrayInitializer(MoArrayInitializer moArrayInitializer) {
        tokens.add("{");
        moArrayInitializer.getExpressions().forEach(expression -> {
            scan("expression", expression);
        });
        tokens.add("}");
    }

    @Override
    public void visitMoArrayType(MoArrayType moArrayType) {
        scan("elementType", moArrayType.getElementType());
        moArrayType.getDimensions().forEach(this::scan);
    }

    @Override
    public void visitMoAssertStatement(MoAssertStatement moAssertStatement) {
        tokens.add("assert");
        scan("expression", moAssertStatement.getExpression());
        moAssertStatement.getMessage().ifPresent(message -> {
            tokens.add(":");
            scan("message", message);
        });
        tokens.add(";");
    }

    @Override
    public void visitMoAssignment(MoAssignment moAssignment) {
        scan("leftHandSide", moAssignment.getLeft());
        scan("operator", moAssignment.getOperator());
        scan("rightHandSide", moAssignment.getRight());
    }

    @Override
    public void visitMoBlock(MoBlock moBlock) {
        tokens.add("{");
        moBlock.getStatements().forEach(this::scan);
        tokens.add("}");
    }

    @Override
    public void visitMoBooleanLiteral(MoBooleanLiteral moBooleanLiteral) {
        tokens.add(moBooleanLiteral.getValue() ? "true" : "false");
    }

    @Override
    public void visitMoBreakStatement(MoBreakStatement moBreakStatement) {
        tokens.add("break");
        moBreakStatement.getBreakLabel().ifPresent(label -> {
            scan("label", label);
        });
        tokens.add(";");
    }

    @Override
    public void visitMoCastExpression(MoCastExpression moCastExpression) {
        tokens.add("(");
        scan("type", moCastExpression.getCastType());
        tokens.add(")");
        scan("expression", moCastExpression.getExpression());
    }

    @Override
    public void visitMoCatchClause(MoCatchClause moCatchClause) {
        tokens.add("catch");
        tokens.add("(");
        scan("exception", moCatchClause.getException());
        tokens.add(")");
        scan("block", moCatchClause.getBody());
    }

    @Override
    public void visitMoCharacterLiteral(MoCharacterLiteral moCharacterLiteral) {
        tokens.add(moCharacterLiteral.getEscapedValue());
    }

    @Override
    public void visitMoClassInstanceCreation(MoClassInstanceCreation moClassInstanceCreation) {
        moClassInstanceCreation.getExpression().ifPresent(expression -> {
            scan("expression", expression);
        });
        tokens.add("new");

        moClassInstanceCreation.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        scan("type", moClassInstanceCreation.getType());

        tokens.add("(");
        moClassInstanceCreation.getArguments().forEach(argument -> {
            scan("argument", argument);
        });
        tokens.add(")");

        moClassInstanceCreation.getAnonymousClassDeclaration().ifPresent(anonymousClassDeclaration -> {
            scan("anonymousClassDeclaration", anonymousClassDeclaration);
        });
    }

    @Override
    public void visitMoCompilationUnit(MoCompilationUnit moCompilationUnit) {
        moCompilationUnit.getPackageDeclaration().ifPresent(moPackageDeclaration -> {
            scan("package", moPackageDeclaration);
        });
        moCompilationUnit.getImports().forEach(moImportDeclaration -> {
            scan("imports", moImportDeclaration);
        });
        moCompilationUnit.getTypes().forEach(moTypeDeclaration -> {
            scan("types", moTypeDeclaration);
        });
    }

    @Override
    public void visitMoConditionalExpression(MoConditionalExpression moConditionalExpression) {
        scan("expression", moConditionalExpression.getCondition());
        tokens.add("?");
        scan("thenExpression", moConditionalExpression.getThenExpression());
        tokens.add(":");
        scan("elseExpression", moConditionalExpression.getElseExpression());
    }

    @Override
    public void visitMoConstructorInvocation(MoConstructorInvocation moConstructorInvocation) {
        moConstructorInvocation.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        tokens.add("this(");
        moConstructorInvocation.getArguments().forEach(argument -> {
            scan("argument", argument);
        });
        tokens.add(")");
        tokens.add(";");
    }

    @Override
    public void visitMoContinueStatement(MoContinueStatement moContinueStatement) {
        tokens.add("continue");
        moContinueStatement.getContinueLabel().ifPresent(label -> {
            scan("label", label);
        });
        tokens.add(";");
    }

    @Override
    public void visitMoDoStatement(MoDoStatement moDoStatement) {
        tokens.add("do");
        scan("body", moDoStatement.getBody());
        tokens.add("while");
        tokens.add("(");
        scan("expression", moDoStatement.getExpression());
        tokens.add(")");
        tokens.add(";");
    }

    @Override
    public void visitMoEmptyStatement(MoEmptyStatement moEmptyStatement) {
        tokens.add(";");
    }

    @Override
    public void visitMoExpressionStatement(MoExpressionStatement moExpressionStatement) {
        scan("expression", moExpressionStatement.getExpression());
        tokens.add(";");
    }

    @Override
    public void visitMoFieldAccess(MoFieldAccess moFieldAccess) {
        scan("expression", moFieldAccess.getExpression());
        tokens.add(".");
        scan("name", moFieldAccess.getName());
    }

    @Override
    public void visitMoFieldDeclaration(MoFieldDeclaration moFieldDeclaration) {
        moFieldDeclaration.getJavadoc().ifPresent(javadoc -> {
            scan("javadoc", javadoc);
        });
        tokenizeExtendedModifier(moFieldDeclaration.getModifiers());
        scan("type", moFieldDeclaration.getType());
        moFieldDeclaration.getFragments().forEach(moVariableDeclarationFragment -> {
            scan("fragments", moVariableDeclarationFragment);
        });
        tokens.add(";");
    }

    @Override
    public void visitMoForStatement(MoForStatement moForStatement) {
        tokens.add("for");
        tokens.add("(");
        moForStatement.getInitializers().forEach(initializer -> {
            scan("initializer", initializer);
        });
        tokens.add(";");
        moForStatement.getCondition().ifPresent(expression -> {
            scan("expression", expression);
        });
        tokens.add(";");
        moForStatement.getUpdaters().forEach(updater -> {
            scan("updater", updater);
        });
        tokens.add(")");
        scan("body", moForStatement.getBody());
    }

    @Override
    public void visitMoIfStatement(MoIfStatement moIfStatement) {
        tokens.add("if");
        tokens.add("(");
        scan("expression", moIfStatement.getCondition());
        tokens.add(")");
        scan("thenStatement", moIfStatement.getThenStatement());
        moIfStatement.getElseStatement().ifPresent(elseStatement -> {
            tokens.add("else");
            scan("elseStatement", elseStatement);
        });
    }

    @Override
    public void visitMoImportDeclaration(MoImportDeclaration moImportDeclaration) {
        tokens.add("import");
        if (moImportDeclaration.isStatic()) {
            tokens.add("static");
        }
        scan("name", moImportDeclaration.getName());
        if (moImportDeclaration.isOnDemand()) {
            tokens.add("*");
        }
        tokens.add(";");
    }

    @Override
    public void visitMoInfixExpression(MoInfixExpression moInfixExpression) {
        scan("leftOperand", moInfixExpression.getLeft());
        scan("operator", moInfixExpression.getOperator());
        scan("rightOperand", moInfixExpression.getRight());
        moInfixExpression.getExtendedOperands().forEach(operand -> {
            tokens.add(moInfixExpression.getOperator().toString());
            scan("operand", operand);
        });
    }

    @Override
    public void visitMoInitializer(MoInitializer moInitializer) {
        moInitializer.getJavadoc().ifPresent(javadoc -> {
            scan("javadoc", javadoc);
        });
        tokenizeExtendedModifier(moInitializer.getModifiers());
        scan("block", moInitializer.getBody());
    }

    @Override
    public void visitMoJavadoc(MoJavadoc moJavadoc) {
        tokens.add("/**");
        moJavadoc.getTagElements().forEach(tag -> {
            scan("tags", tag);
        });
        tokens.add("*/");
    }

    @Override
    public void visitMoLabeledStatement(MoLabeledStatement moLabeledStatement) {
        scan("label", moLabeledStatement.getLabel());
        tokens.add(":");
        scan("statement", moLabeledStatement.getStatement());
    }

    @Override
    public void visitMoMethodDeclaration(MoMethodDeclaration moMethodDeclaration) {
        moMethodDeclaration.getJavadoc().ifPresent(javadoc -> {
            scan("javadoc", javadoc);
        });
        tokenizeExtendedModifier(moMethodDeclaration.getModifiers());

        if(!moMethodDeclaration.isConstructor()) {
            if(moMethodDeclaration.getReturnType().isPresent()) {
                scan("returnType", moMethodDeclaration.getReturnType().get());
            } else {
                logger.error("Method declaration does not have a return type");
            }
        }
        scan("name", moMethodDeclaration.getName());
        tokens.add("(");
        moMethodDeclaration.getParameters().forEach(parameter -> {
            scan("parameter", parameter);
        });
        tokens.add(")");
        if(!moMethodDeclaration.getThrownExceptionTypes().isEmpty()) {
            tokens.add("throws");
            moMethodDeclaration.getThrownExceptionTypes().forEach(exception -> {
                scan("exception", exception);
            });
        }

        if (moMethodDeclaration.getBody().isPresent()) {
            scan("body", moMethodDeclaration.getBody().get());
        } else {
            tokens.add(";");
        }
    }

    @Override
    public void visitMoMethodInvocation(MoMethodInvocation moMethodInvocation) {
        moMethodInvocation.getExpression().ifPresent(expression -> {
            scan("expression", expression);
            tokens.add(".");
        });

        moMethodInvocation.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        scan("name", moMethodInvocation.getName());
        tokens.add("(");
        moMethodInvocation.getArguments().forEach(argument -> {
            scan("argument", argument);
        });
        tokens.add(")");
    }

    @Override
    public void visitMoNullLiteral(MoNullLiteral moNullLiteral) {
        tokens.add("null");
    }

    @Override
    public void visitMoNumberLiteral(MoNumberLiteral moNumberLiteral) {
        tokens.add(moNumberLiteral.getValue());
    }

    @Override
    public void visitMoPackageDeclaration(MoPackageDeclaration moPackageDeclaration) {
        moPackageDeclaration.getJavadoc().ifPresent(javadoc -> {
            scan("javadoc", javadoc);
        });
        tokenizeExtendedModifier(moPackageDeclaration.getAnnotations());
        tokens.add("package");
        scan("name", moPackageDeclaration.getName());
        tokens.add(";");
    }

    @Override
    public void visitMoParenthesizedExpression(MoParenthesizedExpression moParenthesizedExpression) {
        tokens.add("(");
        scan("expression", moParenthesizedExpression.getExpression());
        tokens.add(")");
    }

    @Override
    public void visitMoPostfixExpression(MoPostfixExpression moPostfixExpression) {
        scan("operand", moPostfixExpression.getOperand());
        scan("operator", moPostfixExpression.getOperator());
    }

    @Override
    public void visitMoPrefixExpression(MoPrefixExpression moPrefixExpression) {
        scan("operator", moPrefixExpression.getOperator());
        scan("operand", moPrefixExpression.getOperand());
    }

    @Override
    public void visitMoPrimitiveType(MoPrimitiveType moPrimitiveType) {
        tokens.add(moPrimitiveType.getTypeKind().toString());
    }

    @Override
    public void visitMoQualifiedName(MoQualifiedName moQualifiedName) {
        scan("qualifier", moQualifiedName.getQualifier());
        tokens.add(".");
        scan("name", moQualifiedName.getName());
    }

    @Override
    public void visitMoReturnStatement(MoReturnStatement moReturnStatement) {
        tokens.add("return");
        moReturnStatement.getExpression().ifPresent(expression -> {
            scan("expression", expression);
        });
        tokens.add(";");
    }

    @Override
    public void visitMoSimpleName(MoSimpleName moSimpleName) {
        tokens.add(moSimpleName.getIdentifier());
    }

    @Override
    public void visitMoSimpleType(MoSimpleType moSimpleType) {
        tokenizeExtendedModifier(moSimpleType.getAnnotations());
        scan("name", moSimpleType.getName());
    }

    @Override
    public void visitMoSingleVariableDeclaration(MoSingleVariableDeclaration moSingleVariableDeclaration) {
        tokenizeExtendedModifier(moSingleVariableDeclaration.getModifiers());
        scan("type", moSingleVariableDeclaration.getType());
        if(moSingleVariableDeclaration.isVarargs()) {
            tokenizeExtendedModifier(moSingleVariableDeclaration.getVarargsAnnotations());
            tokens.add("...");
        }
        scan("name", moSingleVariableDeclaration.getName());
        moSingleVariableDeclaration.getCStyleArrayDimensions().forEach(this::scan);
        moSingleVariableDeclaration.getInitializer().ifPresent(initializer -> {
            tokens.add("=");
            scan("initializer", initializer);
        });
    }

    @Override
    public void visitMoStringLiteral(MoStringLiteral moStringLiteral) {
        tokens.add(moStringLiteral.getEscapedValue());
    }

    @Override
    public void visitMoSuperConstructorInvocation(MoSuperConstructorInvocation moSuperConstructorInvocation) {
        moSuperConstructorInvocation.getExpression().ifPresent(expression -> {
            scan("expression", expression);
            tokens.add(".");
        });
        moSuperConstructorInvocation.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        tokens.add("super");
        tokens.add("(");
        moSuperConstructorInvocation.getArguments().forEach(argument -> {
            scan("argument", argument);
        });
        tokens.add(")");
        tokens.add(";");
    }

    @Override
    public void visitMoSuperFieldAccess(MoSuperFieldAccess moSuperFieldAccess) {
        moSuperFieldAccess.getQualifier().ifPresent(qualifier -> {
            scan("qualifier", qualifier);
            tokens.add(".");
        });
        tokens.add("super");
        tokens.add(".");
        scan("name", moSuperFieldAccess.getName());
    }

    @Override
    public void visitMoSuperMethodInvocation(MoSuperMethodInvocation moSuperMethodInvocation) {
        moSuperMethodInvocation.getQualifier().ifPresent(qualifier -> {
            scan("qualifier", qualifier);
            tokens.add(".");
        });
        tokens.add("super");
        tokens.add(".");
        moSuperMethodInvocation.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        scan("name", moSuperMethodInvocation.getName());
        tokens.add("(");
        moSuperMethodInvocation.getArguments().forEach(argument -> {
            scan("argument", argument);
        });
        tokens.add(")");
    }

    @Override
    public void visitMoSwitchCase(MoSwitchCase moSwitchCase) {
        if (moSwitchCase.getExpression().isPresent()) {
            tokens.add("case");
            scan("expression", moSwitchCase.getExpression().get());
        } else {
            tokens.add("default");
        }
        tokens.add(":");
    }

    @Override
    public void visitMoSwitchStatement(MoSwitchStatement moSwitchStatement) {
        tokens.add("switch");
        tokens.add("(");
        scan("expression", moSwitchStatement.getExpression());
        tokens.add(")");
        tokens.add("{");
        moSwitchStatement.getStatements().forEach(this::scan);
        tokens.add("}");
    }

    @Override
    public void visitMoSynchronizedStatement(MoSynchronizedStatement moSynchronizedStatement) {
        tokens.add("synchronized");
        tokens.add("(");
        scan("expression", moSynchronizedStatement.getExpression());
        tokens.add(")");
        scan("body", moSynchronizedStatement.getBlock());
    }

    @Override
    public void visitMoThisExpression(MoThisExpression moThisExpression) {
        moThisExpression.getQualifier().ifPresent(qualifier -> {
            scan("qualifier", qualifier);
            tokens.add(".");
        });
        tokens.add("this");
    }

    @Override
    public void visitMoThrowStatement(MoThrowStatement moThrowStatement) {
        tokens.add("throw");
        scan("expression", moThrowStatement.getExpression());
        tokens.add(";");
    }

    @Override
    public void visitMoTryStatement(MoTryStatement moTryStatement) {
        tokens.add("try");
        if(!moTryStatement.getResources().isEmpty()) {
            tokens.add("(");
            moTryStatement.getResources().forEach(resource -> {
                scan("resource", resource);
            });
            tokens.add(") ");
        }
        scan("body", moTryStatement.getTryBlock());
        moTryStatement.getCatchClauses().forEach(this::scan);
        moTryStatement.getFinallyBlock().ifPresent(finallyBlock -> {
            tokens.add("finally");
            scan("finally", finallyBlock);
        });
    }

    @Override
    public void visitMoTypeDeclaration(MoTypeDeclaration moTypeDeclaration) {
        moTypeDeclaration.getJavadoc().ifPresent(javadoc -> {
            scan("javadoc", javadoc);
        });
        tokenizeExtendedModifier(moTypeDeclaration.getModifiers());
        if(moTypeDeclaration.isInterface()) {
            tokens.add("interface");
        } else {
            tokens.add("class");
        }
        scan("name", moTypeDeclaration.getName());
        moTypeDeclaration.getTypeParameters().forEach(typeParameter -> {
            scan("typeParameter", typeParameter);
        });
        List<MoType> superInterfaceTypes = moTypeDeclaration.getSuperInterfaceTypes();
        if(moTypeDeclaration.isInterface()) {
            if(!superInterfaceTypes.isEmpty()) {
                tokens.add("extends");
            }
            for (MoType superInterfaceType : superInterfaceTypes) {
                scan("superInterfaceType", superInterfaceType);
            }
        } else {
            moTypeDeclaration.getSuperclassType().ifPresent(superType -> {
                tokens.add("extends");
                scan("superClassType", superType);
            });
            if(!superInterfaceTypes.isEmpty()) {
                tokens.add("implements");
            }
            for (MoType superInterfaceType : superInterfaceTypes) {
                scan("superInterfaceType", superInterfaceType);
            }
        }
        tokens.add("{");
        moTypeDeclaration.getBodyDeclarations().forEach(moBodyDeclaration -> {
            scan("bodyDeclarations", moBodyDeclaration);
        });
        tokens.add("}");
    }

    @Override
    public void visitMoTypeDeclarationStatement(MoTypeDeclarationStatement moTypeDeclarationStatement) {
        scan("declaration", moTypeDeclarationStatement.getTypeDeclaration());
    }

    @Override
    public void visitMoTypeLiteral(MoTypeLiteral moTypeLiteral) {
        scan("type", moTypeLiteral.getType());
        tokens.add(".class");
    }

    @Override
    public void visitMoVariableDeclarationExpression(MoVariableDeclarationExpression moVariableDeclarationExpression) {
        tokenizeExtendedModifier(moVariableDeclarationExpression.getModifiers());
        scan("type", moVariableDeclarationExpression.getType());
        moVariableDeclarationExpression.getFragments().forEach(moVariableDeclarationFragment -> {
            scan("fragments", moVariableDeclarationFragment);
        });
    }

    @Override
    public void visitMoVariableDeclarationFragment(MoVariableDeclarationFragment moVariableDeclarationFragment) {
        scan("name", moVariableDeclarationFragment.getName());
        moVariableDeclarationFragment.getCStyleArrayDimensions().forEach(this::scan);
        moVariableDeclarationFragment.getInitializer().ifPresent(initializer -> {
            tokens.add("=");
            scan("initializer", initializer);
        });
    }

    @Override
    public void visitMoVariableDeclarationStatement(MoVariableDeclarationStatement moVariableDeclarationStatement) {
        tokenizeExtendedModifier(moVariableDeclarationStatement.getModifiers());
        scan("type", moVariableDeclarationStatement.getType());
        moVariableDeclarationStatement.getFragments().forEach(moVariableDeclarationFragment -> {
            scan("fragments", moVariableDeclarationFragment);
        });
        tokens.add(";");
    }

    @Override
    public void visitMoWhileStatement(MoWhileStatement moWhileStatement) {
        tokens.add("while");
        tokens.add("(");
        scan("expression", moWhileStatement.getCondition());
        tokens.add(")");
        scan("body", moWhileStatement.getBody());
    }

    @Override
    public void visitMoInstanceofExpression(MoInstanceofExpression moInstanceofExpression) {
        scan("leftOperand", moInstanceofExpression.getLeftOperand());
        tokens.add("instanceof");
        scan("rightOperand", moInstanceofExpression.getRightOperand());
    }

    @Override
    public void visitMoLineComment(MoLineComment moLineComment) {
        tokens.add("//");
        tokens.add(moLineComment.getCommentStr());
    }

    @Override
    public void visitMoBlockComment(MoBlockComment moBlockComment) {
        tokens.add("/*");
        tokens.add(moBlockComment.getCommentStr());
        tokens.add("*/");
    }

    @Override
    public void visitMoTagElement(MoTagElement moTagElement) {
        moTagElement.getTagName().ifPresent(tokens::add);
        moTagElement.getDocFragments().forEach(docFragment -> {
            if(docFragment instanceof MoTextElement textElement) {
                scan("textElement", textElement);
            } else if (docFragment instanceof MoTagElement tagElement) {
                scan("tagElement", tagElement);
            }
        });
    }

    @Override
    public void visitMoTextElement(MoTextElement moTextElement) {
        tokens.add(moTextElement.getText());
    }

    @Override
    public void visitMoEnhancedForStatement(MoEnhancedForStatement moEnhancedForStatement) {
        tokens.add("for");
        tokens.add("(");
        scan("parameter", moEnhancedForStatement.getParameter());
        tokens.add(":");
        scan("expression", moEnhancedForStatement.getExpression());
        tokens.add(")");
        scan("body", moEnhancedForStatement.getBody());
    }

    @Override
    public void visitMoEnumDeclaration(MoEnumDeclaration moEnumDeclaration) {
        moEnumDeclaration.getJavadoc().ifPresent(javadoc -> {
            scan("javadoc", javadoc);
        });
        tokenizeExtendedModifier(moEnumDeclaration.getModifiers());
        tokens.add("enum");
        scan("name", moEnumDeclaration.getName());

        if(!moEnumDeclaration.getSuperInterfaceTypes().isEmpty()) {
            tokens.add("implements");
            moEnumDeclaration.getSuperInterfaceTypes().forEach(superInterfaceType -> {
                scan("superInterfaceType", superInterfaceType);
            });
        }
        tokens.add("{");
        moEnumDeclaration.getEnumConstants().forEach(enumConstant -> {
            scan("enumConstant", enumConstant);
        });

        moEnumDeclaration.getBodyDeclarations().forEach(moBodyDeclaration -> {
            scan("bodyDeclarations", moBodyDeclaration);
        });
        tokens.add("}");
    }

    @Override
    public void visitMoEnumConstantDeclaration(MoEnumConstantDeclaration moEnumConstantDeclaration) {
        moEnumConstantDeclaration.getJavadoc().ifPresent(javadoc -> {
            scan("javadoc", javadoc);
        });
        tokenizeExtendedModifier(moEnumConstantDeclaration.getModifiers());
        scan("name", moEnumConstantDeclaration.getName());
        if(!moEnumConstantDeclaration.getArguments().isEmpty()) {
            tokens.add("(");
            moEnumConstantDeclaration.getArguments().forEach(argument -> {
                scan("argument", argument);
            });
            tokens.add(")");
        }
        if(moEnumConstantDeclaration.getAnonymousClassDeclaration().isPresent()) {
            scan("anonymousClassDeclaration", moEnumConstantDeclaration.getAnonymousClassDeclaration().get());
        }
    }

    @Override
    public void visitMoTypeParameter(MoTypeParameter moTypeParameter) {
        tokenizeExtendedModifier(moTypeParameter.getModifiers());
        scan("name", moTypeParameter.getName());
        if(!moTypeParameter.getTypeBounds().isEmpty()) {
            tokens.add("extends");
            moTypeParameter.getTypeBounds().forEach(typeBound -> {
                scan("typeBound", typeBound);
            });
        }
    }

    @Override
    public void visitMoParameterizedType(MoParameterizedType moParameterizedType) {
        scan("type", moParameterizedType.getType());
        tokens.add("<");
        moParameterizedType.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        tokens.add(">");
    }

    @Override
    public void visitMoQualifiedType(MoQualifiedType moQualifiedType) {
        scan("qualifier", moQualifiedType.getQualifier());
        tokens.add(".");
        scan("name", moQualifiedType.getSimpleName());
    }

    @Override
    public void visitMoWildcardType(MoWildcardType moWildcardType) {
        tokenizeExtendedModifier(moWildcardType.getAnnotations());
        tokens.add("?");
        moWildcardType.getBound().ifPresent(bound -> {
            if(moWildcardType.isUpperBound()) {
                tokens.add("extends");
            } else {
                tokens.add("super");
            }
            scan("bound", bound);
        });
    }

    @Override
    public void visitMoNormalAnnotation(MoNormalAnnotation moNormalAnnotation) {
        tokens.add("@");
        scan("typeName", moNormalAnnotation.getTypeName());
        tokens.add("(");
        moNormalAnnotation.getMemberValuePairs().forEach(memberValuePair -> {
            scan("memberValuePair", memberValuePair);
        });
        tokens.add(")");
    }

    @Override
    public void visitMoMarkerAnnotation(MoMarkerAnnotation moMarkerAnnotation) {
        tokens.add("@");
        scan("typeName", moMarkerAnnotation.getTypeName());
    }

    @Override
    public void visitMoSingleMemberAnnotation(MoSingleMemberAnnotation moSingleMemberAnnotation) {
        tokens.add("@");
        scan("typeName", moSingleMemberAnnotation.getTypeName());
        tokens.add("(");
        scan("value", moSingleMemberAnnotation.getValue());
        tokens.add(")");
    }

    @Override
    public void visitMoMemberValuePair(MoMemberValuePair moMemberValuePair) {
        scan("name", moMemberValuePair.getName());
        tokens.add("=");
        scan("value", moMemberValuePair.getValue());
    }

    @Override
    public void visitMoModifier(MoModifier moModifier) {
        tokens.add(moModifier.getModifierKind().toString());
    }

    @Override
    public void visitMoUnionType(MoUnionType moUnionType) {
        moUnionType.getTypes().forEach(type -> {
            scan("type", type);
        });
    }

    @Override
    public void visitMoDimension(MoDimension moDimension) {
        tokens.add("[]");
    }

    @Override
    public void visitMoLambdaExpression(MoLambdaExpression moLambdaExpression) {
        if (moLambdaExpression.hasParentheses()) {
            tokens.add("(");
            for (int i = 0; i < moLambdaExpression.getParameters().size(); i++) {
                scan("parameter", moLambdaExpression.getParameters().get(i));
            }
            tokens.add(") -> ");
        } else {
            for (int i = 0; i < moLambdaExpression.getParameters().size(); i++) {
                scan("parameter", moLambdaExpression.getParameters().get(i));
            }
            tokens.add(" -> ");
        }
        // must be either a Block or an Expression
        scan("body", moLambdaExpression.getBody());
    }

    @Override
    public void visitMoIntersectionType(MoIntersectionType moIntersectionType) {
        moIntersectionType.getTypes().forEach(type -> {
            scan("type", type);
        });
    }

    @Override
    public void visitMoNameQualifiedType(MoNameQualifiedType moNameQualifiedType) {
        scan("qualifier", moNameQualifiedType.getQualifier());
        tokens.add(".");
        tokenizeExtendedModifier(moNameQualifiedType.getAnnotations());
        scan("name", moNameQualifiedType.getSimpleName());
    }

    @Override
    public void visitMoCreationReference(MoCreationReference moCreationReference) {
        scan("type", moCreationReference.getType());
        moCreationReference.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        tokens.add("::");
        tokens.add("new");
    }

    @Override
    public void visitMoExpressionMethodReference(MoExpressionMethodReference moExpressionMethodReference) {
        scan("expression", moExpressionMethodReference.getExpression());
        moExpressionMethodReference.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        tokens.add("::");
        scan("name", moExpressionMethodReference.getSimpleName());
    }

    @Override
    public void visitMoSuperMethodReference(MoSuperMethodReference moSuperMethodReference) {
        moSuperMethodReference.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
            tokens.add(".");
        });
        tokens.add("super");
        tokens.add("::");
        scan("name", moSuperMethodReference.getSimpleName());
    }

    @Override
    public void visitMoTypeMethodReference(MoTypeMethodReference moTypeMethodReference) {
        scan("type", moTypeMethodReference.getType());
        moTypeMethodReference.getTypeArguments().forEach(typeArgument -> {
            scan("typeArgument", typeArgument);
        });
        tokens.add("::");
        scan("name", moTypeMethodReference.getSimpleName());
    }

    @Override
    public void visitMoInfixOperator(MoInfixOperator moInfixOperator) {
        tokens.add(moInfixOperator.getOperator().toString());
    }

    @Override
    public void visitMoAssignmentOperator(MoAssignmentOperator moAssignmentOperator) {
        tokens.add(moAssignmentOperator.getOperator().toString());
    }

    @Override
    public void visitMoPostfixOperator(MoPostfixOperator moPostfixOperator) {
        tokens.add(moPostfixOperator.getOperator().toString());
    }

    @Override
    public void visitMoPrefixOperator(MoPrefixOperator moPrefixOperator) {
        tokens.add(moPrefixOperator.getOperator().toString());
    }

    private void tokenizeExtendedModifier(List<? extends MoExtendedModifier> extendedModifiers) {
        for (MoExtendedModifier moExtendedModifier : extendedModifiers) {
            if (moExtendedModifier instanceof MoAnnotation annotation) {
                scan("annotation", annotation);
            } else if (moExtendedModifier instanceof MoModifier modifier) {
                scan("modifier", modifier);
            }
        }
    }


    /**
     * split camel case
     * @param tokens tokens original
     * @return split tokens by camel case
     */
    private List<String> camelSplit(List<String> tokens) {
        List<String> result = new ArrayList<>();
        int index ;
        for (String s : tokens) {
            if (s.isEmpty()) continue;
            if (!Character.isDigit(s.charAt(0))) {
                for (index = s.length() - 1; index >= 0; index--) {
                    if (!Character.isDigit(s.charAt(index))) {
                        break;
                    }
                }
                s = s.substring(0, index + 1);
            }
            int lower = 0;
            for(int i = 0; i < s.length(); i++){
                if(Character.isUpperCase(s.charAt(i))){
                    String subName = s.substring(lower, i);
                    lower = i;
                    result.add(subName.toLowerCase());
                } else if(s.charAt(i) == '_'){
                    String subName = s.substring(lower, i);
                    lower = i + 1;
                    result.add(subName.toLowerCase());
                }
            }
            if (lower < s.length()) {
                result.add(s.substring(lower).toLowerCase());
            }
        }
        return result;
    }

}

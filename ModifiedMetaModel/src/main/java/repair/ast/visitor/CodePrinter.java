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
import repair.ast.code.virtual.*;
import repair.ast.declaration.*;

import java.util.List;

public class CodePrinter implements Visitor {
    private static final Logger logger = LoggerFactory.getLogger(CodePrinter.class);
    public final static String NEW_LINE = "\n";
    private final StringBuilder codeBuilder = new StringBuilder();
    public CodePrinter() {
        super();
    }

    public void scan(String role, MoNode moNode) {
        scan(moNode);
    }

    public void scan(MoNode element) {
        if (element != null) {
            element.accept(this);
        }
    }

    public String getCode() {
        return codeBuilder.toString();
    }

    protected CodePrinter write(String value) {
        codeBuilder.append(value);
        return this;
    }


    @Override
    public void visitMoAnonymousClassDeclaration(MoAnonymousClassDeclaration moAnonymousClassDeclaration) {
        write("{ ");
        moAnonymousClassDeclaration.getBodyDeclarations().forEach(moBodyDeclaration -> {
            write(NEW_LINE);
            scan(moBodyDeclaration);
        });
        write("}");
    }

    @Override
    public void visitMoArrayAccess(MoArrayAccess moArrayAccess) {
        scan("array", moArrayAccess.getArray());
        write("[");
        scan("index", moArrayAccess.getIndex());
        write("]");
    }

    @Override
    public void visitMoArrayCreation(MoArrayCreation moArrayCreation) {
        write("new ");
        scan("elementType", moArrayCreation.getType().getElementType());
        moArrayCreation.getDimensionExpressions().forEach(dimensionExpression -> {
            write("[");
            scan("expression", dimensionExpression);
            write("]");
        });

        int expressionSize = moArrayCreation.getDimensionExpressions().size();
        int dimensionSize = moArrayCreation.getType().getDimensions().size();

        for (int i = 0; i < dimensionSize - expressionSize; i++) {
            write("[]");
        }

        moArrayCreation.getInitializer().ifPresent(moArrayInitializer -> {
            write(" ");
            scan("initializer", moArrayInitializer);
        });

    }

    @Override
    public void visitMoArrayInitializer(MoArrayInitializer moArrayInitializer) {
        write("{");
        for (int i = 0; i < moArrayInitializer.getExpressions().size(); i++) {
            scan("expression", moArrayInitializer.getExpressions().get(i));
            if (i < moArrayInitializer.getExpressions().size() - 1) {
                write(", ");
            }
        }
        write("}");
    }

    @Override
    public void visitMoArrayType(MoArrayType moArrayType) {
        scan("elementType", moArrayType.getElementType());
        moArrayType.getDimensions().forEach(this::scan);
    }

    @Override
    public void visitMoAssertStatement(MoAssertStatement moAssertStatement) {
        write("assert ");
        scan("expression", moAssertStatement.getExpression());
        moAssertStatement.getMessage().ifPresent(message -> {
            write(" : ");
            scan("message", message);
        });
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoAssignment(MoAssignment moAssignment) {
        scan("leftHandSide", moAssignment.getLeft());
        scan("operator", moAssignment.getOperator());
        scan("rightHandSide", moAssignment.getRight());
    }

    @Override
    public void visitMoBlock(MoBlock moBlock) {
        write("{").write(NEW_LINE);
        moBlock.getStatements().forEach(this::scan);
        write("}");
    }

    @Override
    public void visitMoBooleanLiteral(MoBooleanLiteral moBooleanLiteral) {
        write(moBooleanLiteral.getValue() ? "true" : "false");
    }

    @Override
    public void visitMoBreakStatement(MoBreakStatement moBreakStatement) {
        write("break");
        moBreakStatement.getBreakLabel().ifPresent(label -> {
            write(" ");
            scan("label", label);
        });
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoCastExpression(MoCastExpression moCastExpression) {
        write("(");
        scan("type", moCastExpression.getCastType());
        write(") ");
        scan("expression", moCastExpression.getExpression());
    }

    @Override
    public void visitMoCatchClause(MoCatchClause moCatchClause) {
        write("catch (");
        scan("exception", moCatchClause.getException());
        write(") ");
        scan("block", moCatchClause.getBody());
    }

    @Override
    public void visitMoCharacterLiteral(MoCharacterLiteral moCharacterLiteral) {
        write(moCharacterLiteral.getEscapedValue());
    }

    @Override
    public void visitMoClassInstanceCreation(MoClassInstanceCreation moClassInstanceCreation) {
        moClassInstanceCreation.getExpression().ifPresent(expression -> {
            scan("expression", expression);
            write(".");
        });
        write("new ");
        if(moClassInstanceCreation.isTypeInferred()) {
            write("<").write(">");
        } else {
            writeTypeArguments(moClassInstanceCreation.getTypeArguments());
        }
        scan("type", moClassInstanceCreation.getType());
        write("(");
        writeArguments(moClassInstanceCreation.getArguments());
        write(")");
        if (moClassInstanceCreation.getAnonymousClassDeclaration().isPresent()) {
            write(" ");
            scan(moClassInstanceCreation.getAnonymousClassDeclaration().get());
        }
    }



    @Override
    public void visitMoCompilationUnit(MoCompilationUnit moCompilationUnit) {
        moCompilationUnit.getPackageDeclaration().ifPresent(moPackageDeclaration -> {
            scan(moPackageDeclaration);
            write(NEW_LINE).write(NEW_LINE);
        });
        moCompilationUnit.getImports().forEach(moImportDeclaration -> {
            scan(moImportDeclaration);
            write(NEW_LINE);
        });
        moCompilationUnit.getTypes().forEach(moTypeDeclaration -> {
            scan(moTypeDeclaration);
            write(NEW_LINE);
        });
    }

    @Override
    public void visitMoConditionalExpression(MoConditionalExpression moConditionalExpression) {
        scan("expression", moConditionalExpression.getCondition());
        write(" ? ");
        scan("thenExpression", moConditionalExpression.getThenExpression());
        write(" : ");
        scan("elseExpression", moConditionalExpression.getElseExpression());
    }

    @Override
    public void visitMoConstructorInvocation(MoConstructorInvocation moConstructorInvocation) {
        writeTypeArguments(moConstructorInvocation.getTypeArguments());
        write("this(");
        writeArguments(moConstructorInvocation.getArguments());
        write(");").write(NEW_LINE);
    }


    @Override
    public void visitMoContinueStatement(MoContinueStatement moContinueStatement) {
        write("continue");
        moContinueStatement.getContinueLabel().ifPresent(label -> {
            write(" ");
            scan("label", label);
        });
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoDoStatement(MoDoStatement moDoStatement) {
        write("do ");
        scan("body", moDoStatement.getBody());
        write(" while (");
        scan("expression", moDoStatement.getExpression());
        write(");").write(NEW_LINE);
    }

    @Override
    public void visitMoEmptyStatement(MoEmptyStatement moEmptyStatement) {
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoExpressionStatement(MoExpressionStatement moExpressionStatement) {
        scan("expression", moExpressionStatement.getExpression());
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoFieldAccess(MoFieldAccess moFieldAccess) {
        scan("expression", moFieldAccess.getExpression());
        write(".");
        scan("name", moFieldAccess.getName());
    }

    @Override
    public void visitMoFieldDeclaration(MoFieldDeclaration moFieldDeclaration) {
        if(moFieldDeclaration.getJavadoc().isPresent()) {
            writeJavadoc(moFieldDeclaration.getJavadoc().get());
        }
        writeExtendedModifier(moFieldDeclaration.getModifiers());

        scan("type", moFieldDeclaration.getType());
        write(" ");
        for (int i = 0; i < moFieldDeclaration.getFragments().size(); i++) {
            scan("fragment", moFieldDeclaration.getFragments().get(i));
            if (i < moFieldDeclaration.getFragments().size() - 1) {
                write(", ");
            }
        }
        write(";").write(NEW_LINE);
    }


    @Override
    public void visitMoForStatement(MoForStatement moForStatement) {
        write("for (");
        for (int i = 0; i < moForStatement.getInitializers().size(); i++) {
            scan("initializer", moForStatement.getInitializers().get(i));
            if (i < moForStatement.getInitializers().size() - 1) {
                write(", ");
            }
        }
        write("; ");
        moForStatement.getCondition().ifPresent(expression -> {
            scan("expression", expression);
        });
        write("; ");

        for (int i = 0; i < moForStatement.getUpdaters().size(); i++) {
            scan("updater", moForStatement.getUpdaters().get(i));
            if (i < moForStatement.getUpdaters().size() - 1) {
                write(", ");
            }
        }
        write(") ");
        scan("body", moForStatement.getBody());
    }

    @Override
    public void visitMoIfStatement(MoIfStatement moIfStatement) {
        write("if (");
        scan("expression", moIfStatement.getCondition());
        write(") ");
        scan("thenStatement", moIfStatement.getThenStatement());
        moIfStatement.getElseStatement().ifPresent(elseStatement -> {
            write(" else ");
            scan("elseStatement", elseStatement);
        });
    }

    @Override
    public void visitMoImportDeclaration(MoImportDeclaration moImportDeclaration) {
        write("import ");
        if(moImportDeclaration.isStatic()) {
            write("static ");
        }
        scan("name", moImportDeclaration.getName());
        if(moImportDeclaration.isOnDemand()) {
            write(".*");
        }
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoInfixExpression(MoInfixExpression moInfixExpression) {
        scan("leftHandSide", moInfixExpression.getLeft());
        scan("operator", moInfixExpression.getOperator());
        scan("rightHandSide", moInfixExpression.getRight());
        if(!moInfixExpression.getExtendedOperands().isEmpty()) {
            for (MoExpression operand : moInfixExpression.getExtendedOperands()) {
                write(moInfixExpression.getOperator().toString());
                scan("operand", operand);
            }
        }

    }

    @Override
    public void visitMoInitializer(MoInitializer moInitializer) {
        moInitializer.getJavadoc().ifPresent(this::writeJavadoc);
        writeExtendedModifier(moInitializer.getModifiers());
        scan("block", moInitializer.getBody());
    }

    @Override
    public void visitMoJavadoc(MoJavadoc moJavadoc) {
        write("/**").write(NEW_LINE);
        moJavadoc.getTagElements().forEach(tagElement -> {
            write("* ");
            scan("tag", tagElement);
        });
        write("*/").write(NEW_LINE);
    }

    @Override
    public void visitMoLabeledStatement(MoLabeledStatement moLabeledStatement) {
        scan("label", moLabeledStatement.getLabel());
        write(": ");
        scan("statement", moLabeledStatement.getStatement());
    }

    @Override
    public void visitMoMethodDeclaration(MoMethodDeclaration moMethodDeclaration) {
        moMethodDeclaration.getJavadoc().ifPresent(this::writeJavadoc);
        writeExtendedModifier(moMethodDeclaration.getModifiers());
        writeTypeParameters(moMethodDeclaration.getTypeParameters());

        if(!moMethodDeclaration.isConstructor()) {
            if(moMethodDeclaration.getReturnType().isPresent()) {
                scan("returnType", moMethodDeclaration.getReturnType().get());
            } else {
                logger.error("Method declaration does not have a return type");
            }
            write(" ");
        }
        scan("name", moMethodDeclaration.getName());
        write("(");
        for (int i = 0; i < moMethodDeclaration.getParameters().size(); i++) {
            scan("parameter", moMethodDeclaration.getParameters().get(i));
            if (i < moMethodDeclaration.getParameters().size() - 1) {
                write(", ");
            }
        }
        write(")");
        if(!moMethodDeclaration.getThrownExceptionTypes().isEmpty()) {
            write(" throws ");
            for (int i = 0; i < moMethodDeclaration.getThrownExceptionTypes().size(); i++) {
                scan("exception", moMethodDeclaration.getThrownExceptionTypes().get(i));
                if (i < moMethodDeclaration.getThrownExceptionTypes().size() - 1) {
                    write(", ");
                }
            }
        }
        if (moMethodDeclaration.getBody().isPresent()) {
            write(" ");
            scan("body", moMethodDeclaration.getBody().get());
        } else {
            write(";").write(NEW_LINE);
        }

    }



    @Override
    public void visitMoMethodInvocation(MoMethodInvocation moMethodInvocation) {
        moMethodInvocation.getTarget().ifPresent(expression -> {
            scan("expression", expression);
            write(".");
        });
        if(moMethodInvocation.isTypeInferred()) {
            write("<").write(">");
        } else {
            writeTypeArguments(moMethodInvocation.getTypeArguments());
        }
        scan("name", moMethodInvocation.getName());
        write("(");
        moMethodInvocation.getArguments().ifPresent(arguments -> {
            scan("arguments", arguments);
        });
        write(")");
    }

    @Override
    public void visitMoNullLiteral(MoNullLiteral moNullLiteral) {
        write(moNullLiteral.getNullValue());
    }

    @Override
    public void visitMoNumberLiteral(MoNumberLiteral moNumberLiteral) {
        write(moNumberLiteral.getValue());
    }

    @Override
    public void visitMoPackageDeclaration(MoPackageDeclaration moPackageDeclaration) {
        moPackageDeclaration.getJavadoc().ifPresent(this::writeJavadoc);
        writeExtendedModifier(moPackageDeclaration.getAnnotations());
        write("package ");
        scan("name", moPackageDeclaration.getName());
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoParenthesizedExpression(MoParenthesizedExpression moParenthesizedExpression) {
        write("(");
        scan("expression", moParenthesizedExpression.getExpression());
        write(")");
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
        write(moPrimitiveType.getTypeKind().toString());
    }

    @Override
    public void visitMoQualifiedName(MoQualifiedName moQualifiedName) {
        if(moQualifiedName.getIdentifier() != null) {
            // 将identifier作为一个整体考虑
            write(moQualifiedName.getIdentifier());
            return;
        }
        scan("qualifier", moQualifiedName.getQualifier());
        write(".");
        scan("name", moQualifiedName.getName());
    }

    @Override
    public void visitMoReturnStatement(MoReturnStatement moReturnStatement) {
        write("return");
        moReturnStatement.getExpression().ifPresent(expression -> {
            write(" ");
            scan("expression", expression);
        });
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoSimpleName(MoSimpleName moSimpleName) {
        write(moSimpleName.getIdentifier());
    }

    @Override
    public void visitMoSimpleType(MoSimpleType moSimpleType) {
        writeExtendedModifier(moSimpleType.getAnnotations());
        scan("name", moSimpleType.getName());
    }

    @Override
    public void visitMoSingleVariableDeclaration(MoSingleVariableDeclaration moSingleVariableDeclaration) {
        writeExtendedModifier(moSingleVariableDeclaration.getModifiers());
        scan("type", moSingleVariableDeclaration.getType());
        if(moSingleVariableDeclaration.isVarargs()) {
            writeExtendedModifier(moSingleVariableDeclaration.getVarargsAnnotations());
            write("...");
        }
        write(" ");
        scan("name", moSingleVariableDeclaration.getName());
        moSingleVariableDeclaration.getCStyleArrayDimensions().forEach(this::scan);
        moSingleVariableDeclaration.getInitializer().ifPresent(initializer -> {
            write(" = ");
            scan("initializer", initializer);
        });
    }

    @Override
    public void visitMoStringLiteral(MoStringLiteral moStringLiteral) {
        write(moStringLiteral.getEscapedValue());
    }

    @Override
    public void visitMoSuperConstructorInvocation(MoSuperConstructorInvocation moSuperConstructorInvocation) {
        moSuperConstructorInvocation.getExpression().ifPresent(expression -> {
            scan("expression", expression);
            write(".");
        });
        writeTypeArguments(moSuperConstructorInvocation.getTypeArguments());
        write("super(");
        writeArguments(moSuperConstructorInvocation.getArguments());
        write(");").write(NEW_LINE);
    }

    @Override
    public void visitMoSuperFieldAccess(MoSuperFieldAccess moSuperFieldAccess) {
        moSuperFieldAccess.getQualifier().ifPresent(qualifier -> {
            scan("qualifier", qualifier);
            write(".");
        });
        write("super").write(".");
        scan("name", moSuperFieldAccess.getName());
    }

    @Override
    public void visitMoSuperMethodInvocation(MoSuperMethodInvocation moSuperMethodInvocation) {
        moSuperMethodInvocation.getQualifier().ifPresent(qualifier -> {
            scan("qualifier", qualifier);
            write(".");
        });
        write("super").write(".");
        if(moSuperMethodInvocation.isTypeInferred()) {
            write("<").write(">");
        } else {
            writeTypeArguments(moSuperMethodInvocation.getTypeArguments());
        }
        scan("name", moSuperMethodInvocation.getName());
        write("(");
        writeArguments(moSuperMethodInvocation.getArguments());
        write(")");
    }

    @Override
    public void visitMoSwitchCase(MoSwitchCase moSwitchCase) {
        if (moSwitchCase.getExpression().isPresent()) {
            write("case ");
            scan("expression", moSwitchCase.getExpression().get());
            write(":").write(NEW_LINE);
        } else {
            write("default:").write(NEW_LINE);
        }
    }

    @Override
    public void visitMoSwitchStatement(MoSwitchStatement moSwitchStatement) {
        write("switch (");
        scan("expression", moSwitchStatement.getExpression());
        write(") {").write(NEW_LINE);
        moSwitchStatement.getStatements().forEach(this::scan);
        write("}");
    }

    @Override
    public void visitMoSynchronizedStatement(MoSynchronizedStatement moSynchronizedStatement) {
        write("synchronized (");
        scan("expression", moSynchronizedStatement.getExpression());
        write(") ");
        scan("body", moSynchronizedStatement.getBlock());
    }

    @Override
    public void visitMoThisExpression(MoThisExpression moThisExpression) {
        moThisExpression.getQualifier().ifPresent(qualifier -> {
            scan("qualifier", qualifier);
            write(".");
        });
        write("this");
    }

    @Override
    public void visitMoThrowStatement(MoThrowStatement moThrowStatement) {
        write("throw ");
        scan("expression", moThrowStatement.getExpression());
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoTryStatement(MoTryStatement moTryStatement) {
        write("try ");
        if(!moTryStatement.getResources().isEmpty()) {
            write("(");
            for (int i = 0; i < moTryStatement.getResources().size(); i++) {
                scan("resource", moTryStatement.getResources().get(i));
                if (i < moTryStatement.getResources().size() - 1) {
                    write("; ");
                }
            }
            write(") ");
        }
        scan("body", moTryStatement.getTryBlock());
        moTryStatement.getCatchClauses().forEach(this::scan);
        moTryStatement.getFinallyBlock().ifPresent(finallyBlock -> {
            write(" finally ");
            scan("finally", finallyBlock);
        });
    }

    @Override
    public void visitMoTypeDeclaration(MoTypeDeclaration moTypeDeclaration) {
        moTypeDeclaration.getJavadoc().ifPresent(this::writeJavadoc);
        writeExtendedModifier(moTypeDeclaration.getModifiers());
        if(moTypeDeclaration.isInterface()) {
            write(" interface ");
        } else {
            write(" class ");
        }
        scan("name", moTypeDeclaration.getName());
        writeTypeParameters(moTypeDeclaration.getTypeParameters());
        List<MoType> superInterfaceTypes = moTypeDeclaration.getSuperInterfaceTypes();
        if(moTypeDeclaration.isInterface()) {
            if(!superInterfaceTypes.isEmpty()) {
                write(" extends ");
            }
            for (int i = 0; i < superInterfaceTypes.size(); i++) {
                scan("superInterfaceType", superInterfaceTypes.get(i));
                if (i < superInterfaceTypes.size() - 1) {
                    write(", ");
                }
            }
        } else {
            moTypeDeclaration.getSuperclassType().ifPresent(superType -> {
                write(" extends ");
                scan("superClassType", superType);
            });
            if(!superInterfaceTypes.isEmpty()) {
                write(" implements ");
            }
            for (int i = 0; i < superInterfaceTypes.size(); i++) {
                scan("superInterfaceType", superInterfaceTypes.get(i));
                if (i < superInterfaceTypes.size() - 1) {
                    write(", ");
                }
            }
        }
        write(" {").write(NEW_LINE);
        moTypeDeclaration.getBodyDeclarations().forEach(moBodyDeclaration -> {
            write(NEW_LINE);
            scan(moBodyDeclaration);
        });
        write("}");


    }

    @Override
    public void visitMoTypeDeclarationStatement(MoTypeDeclarationStatement moTypeDeclarationStatement) {
        scan("declaration", moTypeDeclarationStatement.getTypeDeclaration());
    }

    @Override
    public void visitMoTypeLiteral(MoTypeLiteral moTypeLiteral) {
        scan("type", moTypeLiteral.getType());
        write(".class");
    }

    @Override
    public void visitMoVariableDeclarationExpression(MoVariableDeclarationExpression moVariableDeclarationExpression) {
        writeExtendedModifier(moVariableDeclarationExpression.getModifiers());
        scan("type", moVariableDeclarationExpression.getType());
        write(" ");
        for (int i = 0; i < moVariableDeclarationExpression.getFragments().size(); i++) {
            scan("fragment", moVariableDeclarationExpression.getFragments().get(i));
            if (i < moVariableDeclarationExpression.getFragments().size() - 1) {
                write(", ");
            }
        }
    }

    @Override
    public void visitMoVariableDeclarationFragment(MoVariableDeclarationFragment moVariableDeclarationFragment) {
        scan("name", moVariableDeclarationFragment.getName());
        moVariableDeclarationFragment.getCStyleArrayDimensions().forEach(this::scan);
        moVariableDeclarationFragment.getInitializer().ifPresent(initializer -> {
            write(" = ");
            scan("initializer", initializer);
        });
    }

    @Override
    public void visitMoVariableDeclarationStatement(MoVariableDeclarationStatement moVariableDeclarationStatement) {
        writeExtendedModifier(moVariableDeclarationStatement.getModifiers());
        scan("type", moVariableDeclarationStatement.getType());
        write(" ");
        for (int i = 0; i < moVariableDeclarationStatement.getFragments().size(); i++) {
            scan("fragment", moVariableDeclarationStatement.getFragments().get(i));
            if (i < moVariableDeclarationStatement.getFragments().size() - 1) {
                write(", ");
            }
        }
        write(";").write(NEW_LINE);
    }

    @Override
    public void visitMoWhileStatement(MoWhileStatement moWhileStatement) {
        write("while (");
        scan("expression", moWhileStatement.getCondition());
        write(") ");
        scan("body", moWhileStatement.getBody());
    }

    @Override
    public void visitMoInstanceofExpression(MoInstanceofExpression moInstanceofExpression) {
        scan("leftOperand", moInstanceofExpression.getLeftOperand());
        write(" instanceof ");
        scan("rightOperand", moInstanceofExpression.getRightOperand());
    }

    @Override
    public void visitMoLineComment(MoLineComment moLineComment) {
        write("// ");
        write(moLineComment.getCommentStr());
        write(NEW_LINE);
    }

    @Override
    public void visitMoBlockComment(MoBlockComment moBlockComment) {
        write("/* ");
        write(moBlockComment.getCommentStr());
        write(" */").write(NEW_LINE);
    }

    @Override
    public void visitMoTagElement(MoTagElement moTagElement) {
        write(" ");
        moTagElement.getTagName().ifPresent(tag -> write(tag + " "));
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
        write(moTextElement.getText());
    }

    @Override
    public void visitMoEnhancedForStatement(MoEnhancedForStatement moEnhancedForStatement) {
        write("for (");
        scan("parameter", moEnhancedForStatement.getParameter());
        write(" : ");
        scan("expression", moEnhancedForStatement.getExpression());
        write(") ");
        scan("body", moEnhancedForStatement.getBody());
    }

    @Override
    public void visitMoEnumDeclaration(MoEnumDeclaration moEnumDeclaration) {
        moEnumDeclaration.getJavadoc().ifPresent(this::writeJavadoc);
        writeExtendedModifier(moEnumDeclaration.getModifiers());
        write("enum ");
        scan("name", moEnumDeclaration.getName());
        write(" ");
        if(!moEnumDeclaration.getSuperInterfaceTypes().isEmpty()) {
            write("implements ");
            for (int i = 0; i < moEnumDeclaration.getSuperInterfaceTypes().size(); i++) {
                scan("superInterfaceType", moEnumDeclaration.getSuperInterfaceTypes().get(i));
                if (i < moEnumDeclaration.getSuperInterfaceTypes().size() - 1) {
                    write(", ");
                }
            }
            write(" ");
        }
        write("{").write(NEW_LINE);
        moEnumDeclaration.getEnumConstants().forEach(moEnumConstant -> {
            scan(moEnumConstant);
            write(", ").write(NEW_LINE);
        });
        if(!moEnumDeclaration.getEnumConstants().isEmpty()) {
            write(";");
        }

        moEnumDeclaration.getBodyDeclarations().forEach(moBodyDeclaration -> {
            write(NEW_LINE);
            scan(moBodyDeclaration);
        });
        write("}");
    }

    @Override
    public void visitMoEnumConstantDeclaration(MoEnumConstantDeclaration moEnumConstantDeclaration) {
        moEnumConstantDeclaration.getJavadoc().ifPresent(this::writeJavadoc);
        writeExtendedModifier(moEnumConstantDeclaration.getModifiers());
        scan("name", moEnumConstantDeclaration.getName());
        if(!moEnumConstantDeclaration.getArguments().isEmpty()) {
            write("(");
            writeArguments(moEnumConstantDeclaration.getArguments());
            write(")");
        }
        if(moEnumConstantDeclaration.getAnonymousClassDeclaration().isPresent()) {
            write(" ");
            scan("bodyDeclaration", moEnumConstantDeclaration.getAnonymousClassDeclaration().get());
        }
    }

    @Override
    public void visitMoTypeParameter(MoTypeParameter moTypeParameter) {
        writeExtendedModifier(moTypeParameter.getModifiers());
        scan("name", moTypeParameter.getName());
        if(!moTypeParameter.getTypeBounds().isEmpty()) {
            write(" extends ");
            for (int i = 0; i < moTypeParameter.getTypeBounds().size(); i++) {
                scan("typeBound", moTypeParameter.getTypeBounds().get(i));
                if (i < moTypeParameter.getTypeBounds().size() - 1) {
                    write(" & ");
                }
            }
        }
    }

    @Override
    public void visitMoParameterizedType(MoParameterizedType moParameterizedType) {
        scan("type", moParameterizedType.getType());
        if(moParameterizedType.getTypeArguments().isEmpty()) {
            write("<").write(">");
        } else {
            writeTypeArguments(moParameterizedType.getTypeArguments());
        }
    }

    @Override
    public void visitMoQualifiedType(MoQualifiedType moQualifiedType) {
        scan("qualifier", moQualifiedType.getQualifier());
        write(".");
        writeExtendedModifier(moQualifiedType.getAnnotations());
        scan("name", moQualifiedType.getSimpleName());
    }

    @Override
    public void visitMoWildcardType(MoWildcardType moWildcardType) {
        writeExtendedModifier(moWildcardType.getAnnotations());
        write("?");
        moWildcardType.getBound().ifPresent(bound -> {
            if(moWildcardType.isUpperBound()) {
                write(" extends ");
            } else {
                write(" super ");
            }
            scan("bound", bound);
        });
    }

    @Override
    public void visitMoNormalAnnotation(MoNormalAnnotation moNormalAnnotation) {
        write("@");
        scan("typeName", moNormalAnnotation.getTypeName());
        write("(");
        for (int i = 0; i < moNormalAnnotation.getMemberValuePairs().size(); i++) {
            scan("memberValuePair", moNormalAnnotation.getMemberValuePairs().get(i));
            if (i < moNormalAnnotation.getMemberValuePairs().size() - 1) {
                write(", ");
            }
        }
        write(")");
    }

    @Override
    public void visitMoMarkerAnnotation(MoMarkerAnnotation moMarkerAnnotation) {
        write("@");
        scan("typeName", moMarkerAnnotation.getTypeName());
    }

    @Override
    public void visitMoSingleMemberAnnotation(MoSingleMemberAnnotation moSingleMemberAnnotation) {
        write("@");
        scan("typeName", moSingleMemberAnnotation.getTypeName());
        write("(");
        scan("value", moSingleMemberAnnotation.getValue());
        write(")");
    }

    @Override
    public void visitMoMemberValuePair(MoMemberValuePair moMemberValuePair) {
        scan("name", moMemberValuePair.getName());
        write(" = ");
        scan("value", moMemberValuePair.getValue());
    }

    @Override
    public void visitMoModifier(MoModifier moModifier) {
        write(moModifier.getModifierKind().toString());
    }

    @Override
    public void visitMoUnionType(MoUnionType moUnionType) {
        for (int i = 0; i < moUnionType.getTypes().size(); i++) {
            scan("type", moUnionType.getTypes().get(i));
            if (i < moUnionType.getTypes().size() - 1) {
                write(" | ");
            }
        }
    }

    @Override
    public void visitMoDimension(MoDimension moDimension) {
        if(moDimension.getAnnotations().isEmpty()) {
            write("[]");
            return;
        }
        write(" ");
        moDimension.getAnnotations().forEach(moAnnotation -> {
            scan(moAnnotation);
            write(" ");
        });
        write("[]");
    }

    @Override
    public void visitMoLambdaExpression(MoLambdaExpression moLambdaExpression) {
        if (moLambdaExpression.hasParentheses()) {
            write("(");
            for (int i = 0; i < moLambdaExpression.getParameters().size(); i++) {
                scan("parameter", moLambdaExpression.getParameters().get(i));
                if (i < moLambdaExpression.getParameters().size() - 1) {
                    write(", ");
                }
            }
            write(") -> ");
        } else {
            for (int i = 0; i < moLambdaExpression.getParameters().size(); i++) {
                scan("parameter", moLambdaExpression.getParameters().get(i));
                if (i < moLambdaExpression.getParameters().size() - 1) {
                    write(", ");
                }
            }
            write(" -> ");
        }
        // must be either a Block or an Expression
        scan("body", moLambdaExpression.getBody());
    }

    @Override
    public void visitMoIntersectionType(MoIntersectionType moIntersectionType) {
        for (int i = 0; i < moIntersectionType.getTypes().size(); i++) {
            scan("type", moIntersectionType.getTypes().get(i));
            if (i < moIntersectionType.getTypes().size() - 1) {
                write(" & ");
            }
        }
    }

    @Override
    public void visitMoNameQualifiedType(MoNameQualifiedType moNameQualifiedType) {
        scan("qualifier", moNameQualifiedType.getQualifier());
        write(".");
        writeExtendedModifier(moNameQualifiedType.getAnnotations());
        scan("name", moNameQualifiedType.getSimpleName());
    }

    @Override
    public void visitMoCreationReference(MoCreationReference moCreationReference) {
        scan("type", moCreationReference.getType());
        writeTypeArguments(moCreationReference.getTypeArguments());
        write("::new");
    }

    @Override
    public void visitMoExpressionMethodReference(MoExpressionMethodReference moExpressionMethodReference) {
        scan("expression", moExpressionMethodReference.getExpression());
        writeTypeArguments(moExpressionMethodReference.getTypeArguments());
        write("::");
        scan("name", moExpressionMethodReference.getSimpleName());
    }

    @Override
    public void visitMoSuperMethodReference(MoSuperMethodReference moSuperMethodReference) {
        moSuperMethodReference.getQualifier().ifPresent(qualifier -> {
            scan("qualifier", qualifier);
            write(".");
        });
        writeTypeArguments(moSuperMethodReference.getTypeArguments());
        write("super::");
        scan("name", moSuperMethodReference.getSimpleName());
    }

    @Override
    public void visitMoTypeMethodReference(MoTypeMethodReference moTypeMethodReference) {
        scan("type", moTypeMethodReference.getType());
        writeTypeArguments(moTypeMethodReference.getTypeArguments());
        write("::");
        scan("name", moTypeMethodReference.getSimpleName());
    }

    @Override
    public void visitMoInfixOperator(MoInfixOperator moInfixOperator) {
        write(moInfixOperator.getOperator().toString());
    }

    @Override
    public void visitMoAssignmentOperator(MoAssignmentOperator moAssignmentOperator) {
        write(moAssignmentOperator.getOperator().toString());
    }

    @Override
    public void visitMoPostfixOperator(MoPostfixOperator moPostfixOperator) {
        write(moPostfixOperator.getOperator().toString());
    }

    @Override
    public void visitMoPrefixOperator(MoPrefixOperator moPrefixOperator) {
        write(moPrefixOperator.getOperator().toString());
    }

    @Override
    public void visitMoMethodInvocationTarget(MoMethodInvocationTarget moMethodInvocationTarget) {
        scan("expression", moMethodInvocationTarget.getExpression());
    }

    @Override
    public void visitMoMethodInvocationArguments(MoMethodInvocationArguments moMethodInvocationArguments) {
        writeArguments(moMethodInvocationArguments.getArguments());
    }

    private void writeJavadoc(MoJavadoc javadoc) {
        scan("javadoc", javadoc);
        write(NEW_LINE);
    }

    private void writeExtendedModifier(List<? extends MoExtendedModifier> extendedModifiers) {
        for (MoExtendedModifier moExtendedModifier : extendedModifiers) {
            if (moExtendedModifier instanceof MoAnnotation annotation) {
                scan("annotation", annotation);
            } else if (moExtendedModifier instanceof MoModifier modifier) {
                scan("modifier", modifier);
            }
            write(" ");
        }
    }

    private void writeTypeParameters(List<MoTypeParameter> typeParameters) {
        if(!typeParameters.isEmpty()) {
            write(" <");
            for (int i = 0; i < typeParameters.size(); i++) {
                scan("typeParameter", typeParameters.get(i));
                if (i < typeParameters.size() - 1) {
                    write(", ");
                }
            }
            write("> ");
        }
    }

    private void writeTypeArguments(List<MoType> typeArguments) {
        if (!typeArguments.isEmpty()) {
            write("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                scan("typeArgument", typeArguments.get(i));
                if (i < typeArguments.size() - 1) {
                    write(", ");
                }
            }
            write(">");
        }
    }

    private void writeArguments(List<MoExpression> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            scan("argument", arguments.get(i));
            if (i < arguments.size() - 1) {
                write(", ");
            }
        }
    }
    
}

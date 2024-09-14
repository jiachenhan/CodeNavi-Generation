package repair.ast.visitor;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
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
import repair.ast.role.ChildType;
import repair.ast.role.Description;

import java.util.ArrayDeque;
import java.util.Deque;

public class DeepCopyScanner extends DeepScanner{
    private final static Logger logger = LoggerFactory.getLogger(DeepCopyScanner.class);

    private final MoNode originNode;
    private MoNode rootNode = null;
    private final Deque<MoNode> nodeStack = new ArrayDeque<>();
    private final BidiMap<MoNode, MoNode> copyMap = new DualHashBidiMap<>();

    public DeepCopyScanner(MoNode originNode) {
        this.originNode = originNode;
    }

    public MoNode getCopy() {
        originNode.accept(this);
        return rootNode;
    }

    public BidiMap<MoNode, MoNode> getCopyMap() {
        return copyMap;
    }

    @Override
    public void exit(MoNode node) {
        nodeStack.pop();
    }

    @Override
    public void visitMoAnonymousClassDeclaration(MoAnonymousClassDeclaration moAnonymousClassDeclaration) {
        MoAnonymousClassDeclaration moAnonymousClassDeclarationNew = (MoAnonymousClassDeclaration) moAnonymousClassDeclaration.shallowClone();
        copyMap.put(moAnonymousClassDeclaration, moAnonymousClassDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moAnonymousClassDeclarationNew, moAnonymousClassDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moAnonymousClassDeclarationNew;
        }
        nodeStack.push(moAnonymousClassDeclarationNew);

        super.visitMoAnonymousClassDeclaration(moAnonymousClassDeclaration);
    }

    @Override
    public void visitMoArrayAccess(MoArrayAccess moArrayAccess) {
        MoArrayAccess moArrayAccessNew = (MoArrayAccess) moArrayAccess.shallowClone();
        copyMap.put(moArrayAccess, moArrayAccessNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moArrayAccessNew, moArrayAccess);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moArrayAccessNew;
        }
        nodeStack.push(moArrayAccessNew);

        super.visitMoArrayAccess(moArrayAccess);
    }

    @Override
    public void visitMoArrayCreation(MoArrayCreation moArrayCreation) {
        MoArrayCreation moArrayCreationNew = (MoArrayCreation) moArrayCreation.shallowClone();
        copyMap.put(moArrayCreation, moArrayCreationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moArrayCreationNew, moArrayCreation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moArrayCreationNew;
        }
        nodeStack.push(moArrayCreationNew);

        super.visitMoArrayCreation(moArrayCreation);
    }

    @Override
    public void visitMoArrayInitializer(MoArrayInitializer moArrayInitializer) {
        MoArrayInitializer moArrayInitializerNew = (MoArrayInitializer) moArrayInitializer.shallowClone();
        copyMap.put(moArrayInitializer, moArrayInitializerNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moArrayInitializerNew, moArrayInitializer);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moArrayInitializerNew;
        }
        nodeStack.push(moArrayInitializerNew);

        super.visitMoArrayInitializer(moArrayInitializer);
    }

    @Override
    public void visitMoArrayType(MoArrayType moArrayType) {
        MoArrayType moArrayTypeNew = (MoArrayType) moArrayType.shallowClone();
        copyMap.put(moArrayType, moArrayTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moArrayTypeNew, moArrayType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moArrayTypeNew;
        }
        nodeStack.push(moArrayTypeNew);

        super.visitMoArrayType(moArrayType);
    }

    @Override
    public void visitMoAssertStatement(MoAssertStatement moAssertStatement) {
        MoAssertStatement moAssertStatementNew = (MoAssertStatement) moAssertStatement.shallowClone();
        copyMap.put(moAssertStatement, moAssertStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moAssertStatementNew, moAssertStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moAssertStatementNew;
        }
        nodeStack.push(moAssertStatementNew);

        super.visitMoAssertStatement(moAssertStatement);
    }

    @Override
    public void visitMoAssignment(MoAssignment moAssignment) {
        MoAssignment moAssignmentNew = (MoAssignment) moAssignment.shallowClone();
        copyMap.put(moAssignment, moAssignmentNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moAssignmentNew, moAssignment);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moAssignmentNew;
        }
        nodeStack.push(moAssignmentNew);

        super.visitMoAssignment(moAssignment);
    }

    @Override
    public void visitMoBlock(MoBlock moBlock) {
        MoBlock moBlockNew = (MoBlock) moBlock.shallowClone();
        copyMap.put(moBlock, moBlockNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moBlockNew, moBlock);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moBlockNew;
        }
        nodeStack.push(moBlockNew);

        super.visitMoBlock(moBlock);
    }

    @Override
    public void visitMoBooleanLiteral(MoBooleanLiteral moBooleanLiteral) {
        MoBooleanLiteral moBooleanLiteralNew = (MoBooleanLiteral) moBooleanLiteral.shallowClone();
        copyMap.put(moBooleanLiteral, moBooleanLiteralNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moBooleanLiteralNew, moBooleanLiteral);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moBooleanLiteralNew;
        }
        nodeStack.push(moBooleanLiteralNew);

        super.visitMoBooleanLiteral(moBooleanLiteral);
    }

    @Override
    public void visitMoBreakStatement(MoBreakStatement moBreakStatement) {
        MoBreakStatement moBreakStatementNew = (MoBreakStatement) moBreakStatement.shallowClone();
        copyMap.put(moBreakStatement, moBreakStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moBreakStatementNew, moBreakStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moBreakStatementNew;
        }
        nodeStack.push(moBreakStatementNew);

        super.visitMoBreakStatement(moBreakStatement);
    }

    @Override
    public void visitMoCastExpression(MoCastExpression moCastExpression) {
        MoCastExpression moCastExpressionNew = (MoCastExpression) moCastExpression.shallowClone();
        copyMap.put(moCastExpression, moCastExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moCastExpressionNew, moCastExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moCastExpressionNew;
        }
        nodeStack.push(moCastExpressionNew);

        super.visitMoCastExpression(moCastExpression);
    }

    @Override
    public void visitMoCatchClause(MoCatchClause moCatchClause) {
        MoCatchClause moCatchClauseNew = (MoCatchClause) moCatchClause.shallowClone();
        copyMap.put(moCatchClause, moCatchClauseNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moCatchClauseNew, moCatchClause);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moCatchClauseNew;
        }
        nodeStack.push(moCatchClauseNew);

        super.visitMoCatchClause(moCatchClause);
    }

    @Override
    public void visitMoCharacterLiteral(MoCharacterLiteral moCharacterLiteral) {
        MoCharacterLiteral moCharacterLiteralNew = (MoCharacterLiteral) moCharacterLiteral.shallowClone();
        copyMap.put(moCharacterLiteral, moCharacterLiteralNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moCharacterLiteralNew, moCharacterLiteral);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moCharacterLiteralNew;
        }
        nodeStack.push(moCharacterLiteralNew);

        super.visitMoCharacterLiteral(moCharacterLiteral);
    }

    @Override
    public void visitMoClassInstanceCreation(MoClassInstanceCreation moClassInstanceCreation) {
        MoClassInstanceCreation moClassInstanceCreationNew = (MoClassInstanceCreation) moClassInstanceCreation.shallowClone();
        copyMap.put(moClassInstanceCreation, moClassInstanceCreationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moClassInstanceCreationNew, moClassInstanceCreation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moClassInstanceCreationNew;
        }
        nodeStack.push(moClassInstanceCreationNew);

        super.visitMoClassInstanceCreation(moClassInstanceCreation);
    }

    @Override
    public void visitMoCompilationUnit(MoCompilationUnit moCompilationUnit) {
        MoCompilationUnit moCompilationUnitNew = (MoCompilationUnit) moCompilationUnit.shallowClone();
        copyMap.put(moCompilationUnit, moCompilationUnitNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moCompilationUnitNew, moCompilationUnit);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moCompilationUnitNew;
        }
        nodeStack.push(moCompilationUnitNew);

        super.visitMoCompilationUnit(moCompilationUnit);
    }

    @Override
    public void visitMoConditionalExpression(MoConditionalExpression moConditionalExpression) {
        MoConditionalExpression moConditionalExpressionNew = (MoConditionalExpression) moConditionalExpression.shallowClone();
        copyMap.put(moConditionalExpression, moConditionalExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moConditionalExpressionNew, moConditionalExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moConditionalExpressionNew;
        }
        nodeStack.push(moConditionalExpressionNew);

        super.visitMoConditionalExpression(moConditionalExpression);
    }

    @Override
    public void visitMoConstructorInvocation(MoConstructorInvocation moConstructorInvocation) {
        MoConstructorInvocation moConstructorInvocationNew = (MoConstructorInvocation) moConstructorInvocation.shallowClone();
        copyMap.put(moConstructorInvocation, moConstructorInvocationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moConstructorInvocationNew, moConstructorInvocation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moConstructorInvocationNew;
        }
        nodeStack.push(moConstructorInvocationNew);

        super.visitMoConstructorInvocation(moConstructorInvocation);
    }

    @Override
    public void visitMoContinueStatement(MoContinueStatement moContinueStatement) {
        MoContinueStatement moContinueStatementNew = (MoContinueStatement) moContinueStatement.shallowClone();
        copyMap.put(moContinueStatement, moContinueStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moContinueStatementNew, moContinueStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moContinueStatementNew;
        }
        nodeStack.push(moContinueStatementNew);

        super.visitMoContinueStatement(moContinueStatement);
    }

    @Override
    public void visitMoDoStatement(MoDoStatement moDoStatement) {
        MoDoStatement moDoStatementNew = (MoDoStatement) moDoStatement.shallowClone();
        copyMap.put(moDoStatement, moDoStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moDoStatementNew, moDoStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moDoStatementNew;
        }
        nodeStack.push(moDoStatementNew);

        super.visitMoDoStatement(moDoStatement);
    }

    @Override
    public void visitMoEmptyStatement(MoEmptyStatement moEmptyStatement) {
        MoEmptyStatement moEmptyStatementNew = (MoEmptyStatement) moEmptyStatement.shallowClone();
        copyMap.put(moEmptyStatement, moEmptyStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moEmptyStatementNew, moEmptyStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moEmptyStatementNew;
        }
        nodeStack.push(moEmptyStatementNew);

        super.visitMoEmptyStatement(moEmptyStatement);
    }

    @Override
    public void visitMoExpressionStatement(MoExpressionStatement moExpressionStatement) {
        MoExpressionStatement moExpressionStatementNew = (MoExpressionStatement) moExpressionStatement.shallowClone();
        copyMap.put(moExpressionStatement, moExpressionStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moExpressionStatementNew, moExpressionStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moExpressionStatementNew;
        }
        nodeStack.push(moExpressionStatementNew);

        super.visitMoExpressionStatement(moExpressionStatement);
    }

    @Override
    public void visitMoFieldAccess(MoFieldAccess moFieldAccess) {
        MoFieldAccess moFieldAccessNew = (MoFieldAccess) moFieldAccess.shallowClone();
        copyMap.put(moFieldAccess, moFieldAccessNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moFieldAccessNew, moFieldAccess);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moFieldAccessNew;
        }
        nodeStack.push(moFieldAccessNew);

        super.visitMoFieldAccess(moFieldAccess);
    }

    @Override
    public void visitMoFieldDeclaration(MoFieldDeclaration moFieldDeclaration) {
        MoFieldDeclaration moFieldDeclarationNew = (MoFieldDeclaration) moFieldDeclaration.shallowClone();
        copyMap.put(moFieldDeclaration, moFieldDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moFieldDeclarationNew, moFieldDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moFieldDeclarationNew;
        }
        nodeStack.push(moFieldDeclarationNew);

        super.visitMoFieldDeclaration(moFieldDeclaration);
    }

    @Override
    public void visitMoForStatement(MoForStatement moForStatement) {
        MoForStatement moForStatementNew = (MoForStatement) moForStatement.shallowClone();
        copyMap.put(moForStatement, moForStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moForStatementNew, moForStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moForStatementNew;
        }
        nodeStack.push(moForStatementNew);

        super.visitMoForStatement(moForStatement);
    }

    @Override
    public void visitMoIfStatement(MoIfStatement moIfStatement) {
        MoIfStatement moIfStatementNew = (MoIfStatement) moIfStatement.shallowClone();
        copyMap.put(moIfStatement, moIfStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moIfStatementNew, moIfStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moIfStatementNew;
        }
        nodeStack.push(moIfStatementNew);

        super.visitMoIfStatement(moIfStatement);
    }

    @Override
    public void visitMoImportDeclaration(MoImportDeclaration moImportDeclaration) {
        MoImportDeclaration moImportDeclarationNew = (MoImportDeclaration) moImportDeclaration.shallowClone();
        copyMap.put(moImportDeclaration, moImportDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moImportDeclarationNew, moImportDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moImportDeclarationNew;
        }
        nodeStack.push(moImportDeclarationNew);

        super.visitMoImportDeclaration(moImportDeclaration);
    }

    @Override
    public void visitMoInfixExpression(MoInfixExpression moInfixExpression) {
        MoInfixExpression moInfixExpressionNew = (MoInfixExpression) moInfixExpression.shallowClone();
        copyMap.put(moInfixExpression, moInfixExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moInfixExpressionNew, moInfixExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moInfixExpressionNew;
        }
        nodeStack.push(moInfixExpressionNew);

        super.visitMoInfixExpression(moInfixExpression);
    }

    @Override
    public void visitMoInitializer(MoInitializer moInitializer) {
        MoInitializer moInitializerNew = (MoInitializer) moInitializer.shallowClone();
        copyMap.put(moInitializer, moInitializerNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moInitializerNew, moInitializer);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moInitializerNew;
        }
        nodeStack.push(moInitializerNew);

        super.visitMoInitializer(moInitializer);
    }

    @Override
    public void visitMoJavadoc(MoJavadoc moJavadoc) {
        MoJavadoc moJavadocNew = (MoJavadoc) moJavadoc.shallowClone();
        copyMap.put(moJavadoc, moJavadocNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moJavadocNew, moJavadoc);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moJavadocNew;
        }
        nodeStack.push(moJavadocNew);

        super.visitMoJavadoc(moJavadoc);
    }

    @Override
    public void visitMoLabeledStatement(MoLabeledStatement moLabeledStatement) {
        MoLabeledStatement moLabeledStatementNew = (MoLabeledStatement) moLabeledStatement.shallowClone();
        copyMap.put(moLabeledStatement, moLabeledStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moLabeledStatementNew, moLabeledStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moLabeledStatementNew;
        }
        nodeStack.push(moLabeledStatementNew);

        super.visitMoLabeledStatement(moLabeledStatement);
    }

    @Override
    public void visitMoMethodDeclaration(MoMethodDeclaration moMethodDeclaration) {
        MoMethodDeclaration moMethodDeclarationNew = (MoMethodDeclaration) moMethodDeclaration.shallowClone();
        copyMap.put(moMethodDeclaration, moMethodDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moMethodDeclarationNew, moMethodDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moMethodDeclarationNew;
        }
        nodeStack.push(moMethodDeclarationNew);

        super.visitMoMethodDeclaration(moMethodDeclaration);
    }

    @Override
    public void visitMoMethodInvocation(MoMethodInvocation moMethodInvocation) {
        MoMethodInvocation moMethodInvocationNew = (MoMethodInvocation) moMethodInvocation.shallowClone();
        copyMap.put(moMethodInvocation, moMethodInvocationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moMethodInvocationNew, moMethodInvocation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moMethodInvocationNew;
        }
        nodeStack.push(moMethodInvocationNew);

        super.visitMoMethodInvocation(moMethodInvocation);
    }

    @Override
    public void visitMoNullLiteral(MoNullLiteral moNullLiteral) {
        MoNullLiteral moNullLiteralNew = (MoNullLiteral) moNullLiteral.shallowClone();
        copyMap.put(moNullLiteral, moNullLiteralNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moNullLiteralNew, moNullLiteral);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moNullLiteralNew;
        }
        nodeStack.push(moNullLiteralNew);

        super.visitMoNullLiteral(moNullLiteral);
    }

    @Override
    public void visitMoNumberLiteral(MoNumberLiteral moNumberLiteral) {
        MoNumberLiteral moNumberLiteralNew = (MoNumberLiteral) moNumberLiteral.shallowClone();
        copyMap.put(moNumberLiteral, moNumberLiteralNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moNumberLiteralNew, moNumberLiteral);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moNumberLiteralNew;
        }
        nodeStack.push(moNumberLiteralNew);

        super.visitMoNumberLiteral(moNumberLiteral);
    }

    @Override
    public void visitMoPackageDeclaration(MoPackageDeclaration moPackageDeclaration) {
        MoPackageDeclaration moPackageDeclarationNew = (MoPackageDeclaration) moPackageDeclaration.shallowClone();
        copyMap.put(moPackageDeclaration, moPackageDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moPackageDeclarationNew, moPackageDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moPackageDeclarationNew;
        }
        nodeStack.push(moPackageDeclarationNew);

        super.visitMoPackageDeclaration(moPackageDeclaration);
    }

    @Override
    public void visitMoParenthesizedExpression(MoParenthesizedExpression moParenthesizedExpression) {
        MoParenthesizedExpression moParenthesizedExpressionNew = (MoParenthesizedExpression) moParenthesizedExpression.shallowClone();
        copyMap.put(moParenthesizedExpression, moParenthesizedExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moParenthesizedExpressionNew, moParenthesizedExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moParenthesizedExpressionNew;
        }
        nodeStack.push(moParenthesizedExpressionNew);

        super.visitMoParenthesizedExpression(moParenthesizedExpression);
    }

    @Override
    public void visitMoPostfixExpression(MoPostfixExpression moPostfixExpression) {
        MoPostfixExpression moPostfixExpressionNew = (MoPostfixExpression) moPostfixExpression.shallowClone();
        copyMap.put(moPostfixExpression, moPostfixExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moPostfixExpressionNew, moPostfixExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moPostfixExpressionNew;
        }
        nodeStack.push(moPostfixExpressionNew);

        super.visitMoPostfixExpression(moPostfixExpression);
    }

    @Override
    public void visitMoPrefixExpression(MoPrefixExpression moPrefixExpression) {
        MoPrefixExpression moPrefixExpressionNew = (MoPrefixExpression) moPrefixExpression.shallowClone();
        copyMap.put(moPrefixExpression, moPrefixExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moPrefixExpressionNew, moPrefixExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moPrefixExpressionNew;
        }
        nodeStack.push(moPrefixExpressionNew);

        super.visitMoPrefixExpression(moPrefixExpression);
    }

    @Override
    public void visitMoPrimitiveType(MoPrimitiveType moPrimitiveType) {
        MoPrimitiveType moPrimitiveTypeNew = (MoPrimitiveType) moPrimitiveType.shallowClone();
        copyMap.put(moPrimitiveType, moPrimitiveTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moPrimitiveTypeNew, moPrimitiveType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moPrimitiveTypeNew;
        }
        nodeStack.push(moPrimitiveTypeNew);

        super.visitMoPrimitiveType(moPrimitiveType);
    }

    @Override
    public void visitMoQualifiedName(MoQualifiedName moQualifiedName) {
        MoQualifiedName moQualifiedNameNew = (MoQualifiedName) moQualifiedName.shallowClone();
        copyMap.put(moQualifiedName, moQualifiedNameNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moQualifiedNameNew, moQualifiedName);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moQualifiedNameNew;
        }
        nodeStack.push(moQualifiedNameNew);

        super.visitMoQualifiedName(moQualifiedName);
    }

    @Override
    public void visitMoReturnStatement(MoReturnStatement moReturnStatement) {
        MoReturnStatement moReturnStatementNew = (MoReturnStatement) moReturnStatement.shallowClone();
        copyMap.put(moReturnStatement, moReturnStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moReturnStatementNew, moReturnStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moReturnStatementNew;
        }
        nodeStack.push(moReturnStatementNew);

        super.visitMoReturnStatement(moReturnStatement);
    }

    @Override
    public void visitMoSimpleName(MoSimpleName moSimpleName) {
        MoSimpleName moSimpleNameNew = (MoSimpleName) moSimpleName.shallowClone();
        copyMap.put(moSimpleName, moSimpleNameNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSimpleNameNew, moSimpleName);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSimpleNameNew;
        }
        nodeStack.push(moSimpleNameNew);

        super.visitMoSimpleName(moSimpleName);
    }

    @Override
    public void visitMoSimpleType(MoSimpleType moSimpleType) {
        MoSimpleType moSimpleTypeNew = (MoSimpleType) moSimpleType.shallowClone();
        copyMap.put(moSimpleType, moSimpleTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSimpleTypeNew, moSimpleType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSimpleTypeNew;
        }
        nodeStack.push(moSimpleTypeNew);

        super.visitMoSimpleType(moSimpleType);
    }

    @Override
    public void visitMoSingleVariableDeclaration(MoSingleVariableDeclaration moSingleVariableDeclaration) {
        MoSingleVariableDeclaration moSingleVariableDeclarationNew = (MoSingleVariableDeclaration) moSingleVariableDeclaration.shallowClone();
        copyMap.put(moSingleVariableDeclaration, moSingleVariableDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSingleVariableDeclarationNew, moSingleVariableDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSingleVariableDeclarationNew;
        }
        nodeStack.push(moSingleVariableDeclarationNew);

        super.visitMoSingleVariableDeclaration(moSingleVariableDeclaration);
    }

    @Override
    public void visitMoStringLiteral(MoStringLiteral moStringLiteral) {
        MoStringLiteral moStringLiteralNew = (MoStringLiteral) moStringLiteral.shallowClone();
        copyMap.put(moStringLiteral, moStringLiteralNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moStringLiteralNew, moStringLiteral);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moStringLiteralNew;
        }
        nodeStack.push(moStringLiteralNew);

        super.visitMoStringLiteral(moStringLiteral);
    }

    @Override
    public void visitMoSuperConstructorInvocation(MoSuperConstructorInvocation moSuperConstructorInvocation) {
        MoSuperConstructorInvocation moSuperConstructorInvocationNew = (MoSuperConstructorInvocation) moSuperConstructorInvocation.shallowClone();
        copyMap.put(moSuperConstructorInvocation, moSuperConstructorInvocationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSuperConstructorInvocationNew, moSuperConstructorInvocation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSuperConstructorInvocationNew;
        }
        nodeStack.push(moSuperConstructorInvocationNew);

        super.visitMoSuperConstructorInvocation(moSuperConstructorInvocation);
    }

    @Override
    public void visitMoSuperFieldAccess(MoSuperFieldAccess moSuperFieldAccess) {
        MoSuperFieldAccess moSuperFieldAccessNew = (MoSuperFieldAccess) moSuperFieldAccess.shallowClone();
        copyMap.put(moSuperFieldAccess, moSuperFieldAccessNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSuperFieldAccessNew, moSuperFieldAccess);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSuperFieldAccessNew;
        }
        nodeStack.push(moSuperFieldAccessNew);

        super.visitMoSuperFieldAccess(moSuperFieldAccess);
    }

    @Override
    public void visitMoSuperMethodInvocation(MoSuperMethodInvocation moSuperMethodInvocation) {
        MoSuperMethodInvocation moSuperMethodInvocationNew = (MoSuperMethodInvocation) moSuperMethodInvocation.shallowClone();
        copyMap.put(moSuperMethodInvocation, moSuperMethodInvocationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSuperMethodInvocationNew, moSuperMethodInvocation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSuperMethodInvocationNew;
        }
        nodeStack.push(moSuperMethodInvocationNew);

        super.visitMoSuperMethodInvocation(moSuperMethodInvocation);
    }

    @Override
    public void visitMoSwitchCase(MoSwitchCase moSwitchCase) {
        MoSwitchCase moSwitchCaseNew = (MoSwitchCase) moSwitchCase.shallowClone();
        copyMap.put(moSwitchCase, moSwitchCaseNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSwitchCaseNew, moSwitchCase);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSwitchCaseNew;
        }
        nodeStack.push(moSwitchCaseNew);

        super.visitMoSwitchCase(moSwitchCase);
    }

    @Override
    public void visitMoSwitchStatement(MoSwitchStatement moSwitchStatement) {
        MoSwitchStatement moSwitchStatementNew = (MoSwitchStatement) moSwitchStatement.shallowClone();
        copyMap.put(moSwitchStatement, moSwitchStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSwitchStatementNew, moSwitchStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSwitchStatementNew;
        }
        nodeStack.push(moSwitchStatementNew);

        super.visitMoSwitchStatement(moSwitchStatement);
    }

    @Override
    public void visitMoSynchronizedStatement(MoSynchronizedStatement moSynchronizedStatement) {
        MoSynchronizedStatement moSynchronizedStatementNew = (MoSynchronizedStatement) moSynchronizedStatement.shallowClone();
        copyMap.put(moSynchronizedStatement, moSynchronizedStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSynchronizedStatementNew, moSynchronizedStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSynchronizedStatementNew;
        }
        nodeStack.push(moSynchronizedStatementNew);

        super.visitMoSynchronizedStatement(moSynchronizedStatement);
    }

    @Override
    public void visitMoThisExpression(MoThisExpression moThisExpression) {
        MoThisExpression moThisExpressionNew = (MoThisExpression) moThisExpression.shallowClone();
        copyMap.put(moThisExpression, moThisExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moThisExpressionNew, moThisExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moThisExpressionNew;
        }
        nodeStack.push(moThisExpressionNew);

        super.visitMoThisExpression(moThisExpression);
    }

    @Override
    public void visitMoThrowStatement(MoThrowStatement moThrowStatement) {
        MoThrowStatement moThrowStatementNew = (MoThrowStatement) moThrowStatement.shallowClone();
        copyMap.put(moThrowStatement, moThrowStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moThrowStatementNew, moThrowStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moThrowStatementNew;
        }
        nodeStack.push(moThrowStatementNew);

        super.visitMoThrowStatement(moThrowStatement);
    }

    @Override
    public void visitMoTryStatement(MoTryStatement moTryStatement) {
        MoTryStatement moTryStatementNew = (MoTryStatement) moTryStatement.shallowClone();
        copyMap.put(moTryStatement, moTryStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTryStatementNew, moTryStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTryStatementNew;
        }
        nodeStack.push(moTryStatementNew);

        super.visitMoTryStatement(moTryStatement);
    }

    @Override
    public void visitMoTypeDeclaration(MoTypeDeclaration moTypeDeclaration) {
        MoTypeDeclaration moTypeDeclarationNew = (MoTypeDeclaration) moTypeDeclaration.shallowClone();
        copyMap.put(moTypeDeclaration, moTypeDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTypeDeclarationNew, moTypeDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTypeDeclarationNew;
        }
        nodeStack.push(moTypeDeclarationNew);

        super.visitMoTypeDeclaration(moTypeDeclaration);
    }

    @Override
    public void visitMoTypeDeclarationStatement(MoTypeDeclarationStatement moTypeDeclarationStatement) {
        MoTypeDeclarationStatement moTypeDeclarationStatementNew = (MoTypeDeclarationStatement) moTypeDeclarationStatement.shallowClone();
        copyMap.put(moTypeDeclarationStatement, moTypeDeclarationStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTypeDeclarationStatementNew, moTypeDeclarationStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTypeDeclarationStatementNew;
        }
        nodeStack.push(moTypeDeclarationStatementNew);

        super.visitMoTypeDeclarationStatement(moTypeDeclarationStatement);
    }

    @Override
    public void visitMoTypeLiteral(MoTypeLiteral moTypeLiteral) {
        MoTypeLiteral moTypeLiteralNew = (MoTypeLiteral) moTypeLiteral.shallowClone();
        copyMap.put(moTypeLiteral, moTypeLiteralNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTypeLiteralNew, moTypeLiteral);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTypeLiteralNew;
        }
        nodeStack.push(moTypeLiteralNew);

        super.visitMoTypeLiteral(moTypeLiteral);
    }

    @Override
    public void visitMoVariableDeclarationExpression(MoVariableDeclarationExpression moVariableDeclarationExpression) {
        MoVariableDeclarationExpression moVariableDeclarationExpressionNew = (MoVariableDeclarationExpression) moVariableDeclarationExpression.shallowClone();
        copyMap.put(moVariableDeclarationExpression, moVariableDeclarationExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moVariableDeclarationExpressionNew, moVariableDeclarationExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moVariableDeclarationExpressionNew;
        }
        nodeStack.push(moVariableDeclarationExpressionNew);

        super.visitMoVariableDeclarationExpression(moVariableDeclarationExpression);
    }

    @Override
    public void visitMoVariableDeclarationFragment(MoVariableDeclarationFragment moVariableDeclarationFragment) {
        MoVariableDeclarationFragment moVariableDeclarationFragmentNew = (MoVariableDeclarationFragment) moVariableDeclarationFragment.shallowClone();
        copyMap.put(moVariableDeclarationFragment, moVariableDeclarationFragmentNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moVariableDeclarationFragmentNew, moVariableDeclarationFragment);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moVariableDeclarationFragmentNew;
        }
        nodeStack.push(moVariableDeclarationFragmentNew);

        super.visitMoVariableDeclarationFragment(moVariableDeclarationFragment);
    }

    @Override
    public void visitMoVariableDeclarationStatement(MoVariableDeclarationStatement moVariableDeclarationStatement) {
        MoVariableDeclarationStatement moVariableDeclarationStatementNew = (MoVariableDeclarationStatement) moVariableDeclarationStatement.shallowClone();
        copyMap.put(moVariableDeclarationStatement, moVariableDeclarationStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moVariableDeclarationStatementNew, moVariableDeclarationStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moVariableDeclarationStatementNew;
        }
        nodeStack.push(moVariableDeclarationStatementNew);

        super.visitMoVariableDeclarationStatement(moVariableDeclarationStatement);
    }

    @Override
    public void visitMoWhileStatement(MoWhileStatement moWhileStatement) {
        MoWhileStatement moWhileStatementNew = (MoWhileStatement) moWhileStatement.shallowClone();
        copyMap.put(moWhileStatement, moWhileStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moWhileStatementNew, moWhileStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moWhileStatementNew;
        }
        nodeStack.push(moWhileStatementNew);

        super.visitMoWhileStatement(moWhileStatement);
    }

    @Override
    public void visitMoInstanceofExpression(MoInstanceofExpression moInstanceofExpression) {
        MoInstanceofExpression moInstanceofExpressionNew = (MoInstanceofExpression) moInstanceofExpression.shallowClone();
        copyMap.put(moInstanceofExpression, moInstanceofExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moInstanceofExpressionNew, moInstanceofExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moInstanceofExpressionNew;
        }
        nodeStack.push(moInstanceofExpressionNew);

        super.visitMoInstanceofExpression(moInstanceofExpression);
    }

    @Override
    public void visitMoLineComment(MoLineComment moLineComment) {
        MoLineComment moLineCommentNew = (MoLineComment) moLineComment.shallowClone();
        copyMap.put(moLineComment, moLineCommentNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moLineCommentNew, moLineComment);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moLineCommentNew;
        }
        nodeStack.push(moLineCommentNew);

        super.visitMoLineComment(moLineComment);
    }

    @Override
    public void visitMoBlockComment(MoBlockComment moBlockComment) {
        MoBlockComment moBlockCommentNew = (MoBlockComment) moBlockComment.shallowClone();
        copyMap.put(moBlockComment, moBlockCommentNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moBlockCommentNew, moBlockComment);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moBlockCommentNew;
        }
        nodeStack.push(moBlockCommentNew);

        super.visitMoBlockComment(moBlockComment);
    }

    @Override
    public void visitMoTagElement(MoTagElement moTagElement) {
        MoTagElement moTagElementNew = (MoTagElement) moTagElement.shallowClone();
        copyMap.put(moTagElement, moTagElementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTagElementNew, moTagElement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTagElementNew;
        }
        nodeStack.push(moTagElementNew);

        super.visitMoTagElement(moTagElement);
    }

    @Override
    public void visitMoTextElement(MoTextElement moTextElement) {
        MoTextElement moTextElementNew = (MoTextElement) moTextElement.shallowClone();
        copyMap.put(moTextElement, moTextElementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTextElementNew, moTextElement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTextElementNew;
        }
        nodeStack.push(moTextElementNew);

        super.visitMoTextElement(moTextElement);
    }

    @Override
    public void visitMoEnhancedForStatement(MoEnhancedForStatement moEnhancedForStatement) {
        MoEnhancedForStatement moEnhancedForStatementNew = (MoEnhancedForStatement) moEnhancedForStatement.shallowClone();
        copyMap.put(moEnhancedForStatement, moEnhancedForStatementNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moEnhancedForStatementNew, moEnhancedForStatement);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moEnhancedForStatementNew;
        }
        nodeStack.push(moEnhancedForStatementNew);

        super.visitMoEnhancedForStatement(moEnhancedForStatement);
    }

    @Override
    public void visitMoEnumDeclaration(MoEnumDeclaration moEnumDeclaration) {
        MoEnumDeclaration moEnumDeclarationNew = (MoEnumDeclaration) moEnumDeclaration.shallowClone();
        copyMap.put(moEnumDeclaration, moEnumDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moEnumDeclarationNew, moEnumDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moEnumDeclarationNew;
        }
        nodeStack.push(moEnumDeclarationNew);

        super.visitMoEnumDeclaration(moEnumDeclaration);
    }

    @Override
    public void visitMoEnumConstantDeclaration(MoEnumConstantDeclaration moEnumConstantDeclaration) {
        MoEnumConstantDeclaration moEnumConstantDeclarationNew = (MoEnumConstantDeclaration) moEnumConstantDeclaration.shallowClone();
        copyMap.put(moEnumConstantDeclaration, moEnumConstantDeclarationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moEnumConstantDeclarationNew, moEnumConstantDeclaration);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moEnumConstantDeclarationNew;
        }
        nodeStack.push(moEnumConstantDeclarationNew);

        super.visitMoEnumConstantDeclaration(moEnumConstantDeclaration);
    }

    @Override
    public void visitMoTypeParameter(MoTypeParameter moTypeParameter) {
        MoTypeParameter moTypeParameterNew = (MoTypeParameter) moTypeParameter.shallowClone();
        copyMap.put(moTypeParameter, moTypeParameterNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTypeParameterNew, moTypeParameter);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTypeParameterNew;
        }
        nodeStack.push(moTypeParameterNew);

        super.visitMoTypeParameter(moTypeParameter);
    }

    @Override
    public void visitMoParameterizedType(MoParameterizedType moParameterizedType) {
        MoParameterizedType moParameterizedTypeNew = (MoParameterizedType) moParameterizedType.shallowClone();
        copyMap.put(moParameterizedType, moParameterizedTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moParameterizedTypeNew, moParameterizedType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moParameterizedTypeNew;
        }
        nodeStack.push(moParameterizedTypeNew);

        super.visitMoParameterizedType(moParameterizedType);
    }

    @Override
    public void visitMoQualifiedType(MoQualifiedType moQualifiedType) {
        MoQualifiedType moQualifiedTypeNew = (MoQualifiedType) moQualifiedType.shallowClone();
        copyMap.put(moQualifiedType, moQualifiedTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moQualifiedTypeNew, moQualifiedType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moQualifiedTypeNew;
        }
        nodeStack.push(moQualifiedTypeNew);

        super.visitMoQualifiedType(moQualifiedType);
    }

    @Override
    public void visitMoWildcardType(MoWildcardType moWildcardType) {
        MoWildcardType moWildcardTypeNew = (MoWildcardType) moWildcardType.shallowClone();
        copyMap.put(moWildcardType, moWildcardTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moWildcardTypeNew, moWildcardType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moWildcardTypeNew;
        }
        nodeStack.push(moWildcardTypeNew);

        super.visitMoWildcardType(moWildcardType);
    }

    @Override
    public void visitMoNormalAnnotation(MoNormalAnnotation moNormalAnnotation) {
        MoNormalAnnotation moNormalAnnotationNew = (MoNormalAnnotation) moNormalAnnotation.shallowClone();
        copyMap.put(moNormalAnnotation, moNormalAnnotationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moNormalAnnotationNew, moNormalAnnotation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moNormalAnnotationNew;
        }
        nodeStack.push(moNormalAnnotationNew);

        super.visitMoNormalAnnotation(moNormalAnnotation);
    }

    @Override
    public void visitMoMarkerAnnotation(MoMarkerAnnotation moMarkerAnnotation) {
        MoMarkerAnnotation moMarkerAnnotationNew = (MoMarkerAnnotation) moMarkerAnnotation.shallowClone();
        copyMap.put(moMarkerAnnotation, moMarkerAnnotationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moMarkerAnnotationNew, moMarkerAnnotation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moMarkerAnnotationNew;
        }
        nodeStack.push(moMarkerAnnotationNew);

        super.visitMoMarkerAnnotation(moMarkerAnnotation);
    }

    @Override
    public void visitMoSingleMemberAnnotation(MoSingleMemberAnnotation moSingleMemberAnnotation) {
        MoSingleMemberAnnotation moSingleMemberAnnotationNew = (MoSingleMemberAnnotation) moSingleMemberAnnotation.shallowClone();
        copyMap.put(moSingleMemberAnnotation, moSingleMemberAnnotationNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSingleMemberAnnotationNew, moSingleMemberAnnotation);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSingleMemberAnnotationNew;
        }
        nodeStack.push(moSingleMemberAnnotationNew);

        super.visitMoSingleMemberAnnotation(moSingleMemberAnnotation);
    }

    @Override
    public void visitMoMemberValuePair(MoMemberValuePair moMemberValuePair) {
        MoMemberValuePair moMemberValuePairNew = (MoMemberValuePair) moMemberValuePair.shallowClone();
        copyMap.put(moMemberValuePair, moMemberValuePairNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moMemberValuePairNew, moMemberValuePair);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moMemberValuePairNew;
        }
        nodeStack.push(moMemberValuePairNew);

        super.visitMoMemberValuePair(moMemberValuePair);
    }

    @Override
    public void visitMoModifier(MoModifier moModifier) {
        MoModifier moModifierNew = ((MoModifier) moModifier.shallowClone());
        copyMap.put(moModifier, moModifierNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moModifierNew, moModifier);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moModifierNew;
        }
        nodeStack.push(moModifierNew);

        super.visitMoModifier(moModifier);
    }

    @Override
    public void visitMoUnionType(MoUnionType moUnionType) {
        MoUnionType moUnionTypeNew = (MoUnionType) moUnionType.shallowClone();
        copyMap.put(moUnionType, moUnionTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moUnionTypeNew, moUnionType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moUnionTypeNew;
        }
        nodeStack.push(moUnionTypeNew);

        super.visitMoUnionType(moUnionType);
    }

    @Override
    public void visitMoDimension(MoDimension moDimension) {
        MoDimension moDimensionNew = (MoDimension) moDimension.shallowClone();
        copyMap.put(moDimension, moDimensionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moDimensionNew, moDimension);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moDimensionNew;
        }
        nodeStack.push(moDimensionNew);

        super.visitMoDimension(moDimension);
    }

    @Override
    public void visitMoLambdaExpression(MoLambdaExpression moLambdaExpression) {
        MoLambdaExpression moLambdaExpressionNew = (MoLambdaExpression) moLambdaExpression.shallowClone();
        copyMap.put(moLambdaExpression, moLambdaExpressionNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moLambdaExpressionNew, moLambdaExpression);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moLambdaExpressionNew;
        }
        nodeStack.push(moLambdaExpressionNew);

        super.visitMoLambdaExpression(moLambdaExpression);
    }

    @Override
    public void visitMoIntersectionType(MoIntersectionType moIntersectionType) {
        MoIntersectionType moIntersectionTypeNew = (MoIntersectionType) moIntersectionType.shallowClone();
        copyMap.put(moIntersectionType, moIntersectionTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moIntersectionTypeNew, moIntersectionType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moIntersectionTypeNew;
        }
        nodeStack.push(moIntersectionTypeNew);

        super.visitMoIntersectionType(moIntersectionType);
    }

    @Override
    public void visitMoNameQualifiedType(MoNameQualifiedType moNameQualifiedType) {
        MoNameQualifiedType moNameQualifiedTypeNew = (MoNameQualifiedType) moNameQualifiedType.shallowClone();
        copyMap.put(moNameQualifiedType, moNameQualifiedTypeNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moNameQualifiedTypeNew, moNameQualifiedType);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moNameQualifiedTypeNew;
        }
        nodeStack.push(moNameQualifiedTypeNew);

        super.visitMoNameQualifiedType(moNameQualifiedType);
    }

    @Override
    public void visitMoCreationReference(MoCreationReference moCreationReference) {
        MoCreationReference moCreationReferenceNew = (MoCreationReference) moCreationReference.shallowClone();
        copyMap.put(moCreationReference, moCreationReferenceNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moCreationReferenceNew, moCreationReference);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moCreationReferenceNew;
        }
        nodeStack.push(moCreationReferenceNew);

        super.visitMoCreationReference(moCreationReference);
    }

    @Override
    public void visitMoExpressionMethodReference(MoExpressionMethodReference moExpressionMethodReference) {
        MoExpressionMethodReference moExpressionMethodReferenceNew = (MoExpressionMethodReference) moExpressionMethodReference.shallowClone();
        copyMap.put(moExpressionMethodReference, moExpressionMethodReferenceNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moExpressionMethodReferenceNew, moExpressionMethodReference);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moExpressionMethodReferenceNew;
        }
        nodeStack.push(moExpressionMethodReferenceNew);

        super.visitMoExpressionMethodReference(moExpressionMethodReference);
    }

    @Override
    public void visitMoSuperMethodReference(MoSuperMethodReference moSuperMethodReference) {
        MoSuperMethodReference moSuperMethodReferenceNew = (MoSuperMethodReference) moSuperMethodReference.shallowClone();
        copyMap.put(moSuperMethodReference, moSuperMethodReferenceNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moSuperMethodReferenceNew, moSuperMethodReference);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moSuperMethodReferenceNew;
        }
        nodeStack.push(moSuperMethodReferenceNew);

        super.visitMoSuperMethodReference(moSuperMethodReference);
    }

    @Override
    public void visitMoTypeMethodReference(MoTypeMethodReference moTypeMethodReference) {
        MoTypeMethodReference moTypeMethodReferenceNew = (MoTypeMethodReference) moTypeMethodReference.shallowClone();
        copyMap.put(moTypeMethodReference, moTypeMethodReferenceNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moTypeMethodReferenceNew, moTypeMethodReference);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moTypeMethodReferenceNew;
        }
        nodeStack.push(moTypeMethodReferenceNew);

        super.visitMoTypeMethodReference(moTypeMethodReference);
    }

    @Override
    public void visitMoInfixOperator(MoInfixOperator moInfixOperator) {
        MoInfixOperator moInfixOperatorNew = (MoInfixOperator) moInfixOperator.shallowClone();
        copyMap.put(moInfixOperator, moInfixOperatorNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moInfixOperatorNew, moInfixOperator);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moInfixOperatorNew;
        }
        nodeStack.push(moInfixOperatorNew);

        super.visitMoInfixOperator(moInfixOperator);
    }

    @Override
    public void visitMoAssignmentOperator(MoAssignmentOperator moAssignmentOperator) {
        MoAssignmentOperator moAssignmentOperatorNew = (MoAssignmentOperator) moAssignmentOperator.shallowClone();
        copyMap.put(moAssignmentOperator, moAssignmentOperatorNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moAssignmentOperatorNew, moAssignmentOperator);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moAssignmentOperatorNew;
        }
        nodeStack.push(moAssignmentOperatorNew);

        super.visitMoAssignmentOperator(moAssignmentOperator);
    }

    @Override
    public void visitMoPostfixOperator(MoPostfixOperator moPostfixOperator) {
        MoPostfixOperator moPostfixOperatorNew = (MoPostfixOperator) moPostfixOperator.shallowClone();
        copyMap.put(moPostfixOperator, moPostfixOperatorNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moPostfixOperatorNew, moPostfixOperator);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moPostfixOperatorNew;
        }
        nodeStack.push(moPostfixOperatorNew);

        super.visitMoPostfixOperator(moPostfixOperator);
    }

    @Override
    public void visitMoPrefixOperator(MoPrefixOperator moPrefixOperator) {
        MoPrefixOperator moPrefixOperatorNew = (MoPrefixOperator) moPrefixOperator.shallowClone();
        copyMap.put(moPrefixOperator, moPrefixOperatorNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moPrefixOperatorNew, moPrefixOperator);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moPrefixOperatorNew;
        }
        nodeStack.push(moPrefixOperatorNew);

        super.visitMoPrefixOperator(moPrefixOperator);
    }

    @Override
    public void visitMoMethodInvocationTarget(MoMethodInvocationTarget moMethodInvocationTarget) {
        MoMethodInvocationTarget moMethodInvocationTargetNew = (MoMethodInvocationTarget) moMethodInvocationTarget.shallowClone();
        copyMap.put(moMethodInvocationTarget, moMethodInvocationTargetNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moMethodInvocationTargetNew, moMethodInvocationTarget);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moMethodInvocationTargetNew;
        }
        nodeStack.push(moMethodInvocationTargetNew);

        super.visitMoMethodInvocationTarget(moMethodInvocationTarget);
    }

    @Override
    public void visitMoMethodInvocationArguments(MoMethodInvocationArguments moMethodInvocationArguments) {
        MoMethodInvocationArguments moMethodInvocationArgumentsNew = (MoMethodInvocationArguments) moMethodInvocationArguments.shallowClone();
        copyMap.put(moMethodInvocationArguments, moMethodInvocationArgumentsNew);

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, moMethodInvocationArgumentsNew, moMethodInvocationArguments);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = moMethodInvocationArgumentsNew;
        }
        nodeStack.push(moMethodInvocationArgumentsNew);

        super.visitMoMethodInvocationArguments(moMethodInvocationArguments);
    }

    private void bindingParentChildRelation(MoNode moParent, MoNode moChild, MoNode oriChild) {
        Description<? extends MoNode, ?> originLocationInParent = oriChild.getLocationInParent();
        moChild.setParent(moParent, originLocationInParent);

        if (originLocationInParent.classification() == ChildType.CHILDLIST) {
            if (isSubclassOrSameClass(originLocationInParent.childNodeType(), oriChild.getClass())) {
                moParent.addStructuralPropertyList(originLocationInParent.role(), moChild);
            } else {
                logger.error("childList element type is not {} in {}", moChild.getClass(), moParent);
            }
        } else if(originLocationInParent.classification() == ChildType.CHILD) {
            if (isSubclassOrSameClass(originLocationInParent.childNodeType(), oriChild.getClass())) {
                moParent.setStructuralProperty(originLocationInParent.role(), moChild);

            } else {
                logger.error("child type is not {} in {}", moChild.getClass(), moParent);
            }
        } else {
            logger.error("{} is not a child of {}", moChild.getClass(), moParent);
        }
    }

    private boolean isSubclassOrSameClass(Class<?> superClass, Class<?> subClass) {
        return superClass.isAssignableFrom(subClass);
    }

}

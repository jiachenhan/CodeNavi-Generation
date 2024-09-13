package repair.modify.builder;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Type;
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
import repair.ast.visitor.DeepScanner;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.github.gumtreediff.tree.TypeSet.type;

public class MoGumtreeScanner extends DeepScanner {
    // todo: Need to refer to gumtree.gen.jdt for optimization
    private final static Logger logger = LoggerFactory.getLogger(MoGumtreeScanner.class);
    public static final String NOTYPE = "<noType>";
    private final TreeContext treeContext;
    private final Deque<Tree> stack = new ArrayDeque<>();

    public MoGumtreeScanner(TreeContext treeContext, Tree root) {
        this.treeContext = treeContext;
        stack.push(root);
    }

    private void pushToStack(Tree tree) {
        if(stack.isEmpty()){
            logger.error("Stack should not be empty");
            return;
        }
        Tree parent = stack.peek();
        parent.addChild(tree);
        stack.push(tree);
    }

    @Override
    protected void exit(MoNode node) {
        stack.pop();
    }

    @Override
    public void visitMoAnonymousClassDeclaration(MoAnonymousClassDeclaration moAnonymousClassDeclaration) {
        String nodeTypeName = getNodeType(moAnonymousClassDeclaration);
        String label = "";
        Tree tree = createNode(nodeTypeName, moAnonymousClassDeclaration, label);
        pushToStack(tree);

        super.visitMoAnonymousClassDeclaration(moAnonymousClassDeclaration);
    }

    @Override
    public void visitMoArrayAccess(MoArrayAccess moArrayAccess) {
        String nodeTypeName = getNodeType(moArrayAccess);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moArrayAccess, label);
        pushToStack(newNode);

        super.visitMoArrayAccess(moArrayAccess);
    }

    @Override
    public void visitMoArrayCreation(MoArrayCreation moArrayCreation) {
        String nodeTypeName = getNodeType(moArrayCreation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moArrayCreation, label);
        pushToStack(newNode);

        super.visitMoArrayCreation(moArrayCreation);
    }

    @Override
    public void visitMoArrayInitializer(MoArrayInitializer moArrayInitializer) {
        String nodeTypeName = getNodeType(moArrayInitializer);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moArrayInitializer, label);
        pushToStack(newNode);

        super.visitMoArrayInitializer(moArrayInitializer);
    }

    @Override
    public void visitMoArrayType(MoArrayType moArrayType) {
        String nodeTypeName = getNodeType(moArrayType);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moArrayType, label);
        pushToStack(newNode);

        super.visitMoArrayType(moArrayType);
    }

    @Override
    public void visitMoAssertStatement(MoAssertStatement moAssertStatement) {
        String nodeTypeName = getNodeType(moAssertStatement);
        String label = "assert";
        Tree newNode = createNode(nodeTypeName, moAssertStatement, label);
        pushToStack(newNode);

        super.visitMoAssertStatement(moAssertStatement);
    }

    @Override
    public void visitMoAssignment(MoAssignment moAssignment) {
        String nodeTypeName = getNodeType(moAssignment);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moAssignment, label);
        pushToStack(newNode);

        super.visitMoAssignment(moAssignment);
    }

    @Override
    public void visitMoBlock(MoBlock moBlock) {
        String nodeTypeName = getNodeType(moBlock);
        String label = "{";
        if("thenStatement".equals(moBlock.getLocationInParent().role())) {
            label = "then";
        } else if("elseStatement".equals(moBlock.getLocationInParent().role())) {
            label = "else";
        }
        Tree newNode = createNode(nodeTypeName, moBlock, label);
        pushToStack(newNode);

        super.visitMoBlock(moBlock);
    }

    @Override
    public void visitMoBooleanLiteral(MoBooleanLiteral moBooleanLiteral) {
        String nodeTypeName = getNodeType(moBooleanLiteral);
        String label = moBooleanLiteral.getValue() ? "true" : "false";
        Tree newNode = createNode(nodeTypeName, moBooleanLiteral, label);
        pushToStack(newNode);

        super.visitMoBooleanLiteral(moBooleanLiteral);
    }

    @Override
    public void visitMoBreakStatement(MoBreakStatement moBreakStatement) {
        String nodeTypeName = getNodeType(moBreakStatement);
        String label = "break";
        Tree newNode = createNode(nodeTypeName, moBreakStatement, label);
        pushToStack(newNode);

        super.visitMoBreakStatement(moBreakStatement);
    }

    @Override
    public void visitMoCastExpression(MoCastExpression moCastExpression) {
        String nodeTypeName = getNodeType(moCastExpression);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moCastExpression, label);
        pushToStack(newNode);

        super.visitMoCastExpression(moCastExpression);
    }

    @Override
    public void visitMoCatchClause(MoCatchClause moCatchClause) {
        String nodeTypeName = getNodeType(moCatchClause);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moCatchClause, label);
        pushToStack(newNode);

        super.visitMoCatchClause(moCatchClause);
    }

    @Override
    public void visitMoCharacterLiteral(MoCharacterLiteral moCharacterLiteral) {
        String nodeTypeName = getNodeType(moCharacterLiteral);
        String label = moCharacterLiteral.getEscapedValue();
        Tree newNode = createNode(nodeTypeName, moCharacterLiteral, label);
        pushToStack(newNode);

        super.visitMoCharacterLiteral(moCharacterLiteral);
    }

    @Override
    public void visitMoClassInstanceCreation(MoClassInstanceCreation moClassInstanceCreation) {
        String nodeTypeName = getNodeType(moClassInstanceCreation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moClassInstanceCreation, label);
        pushToStack(newNode);

        super.visitMoClassInstanceCreation(moClassInstanceCreation);
    }

    @Override
    public void visitMoCompilationUnit(MoCompilationUnit moCompilationUnit) {
        String nodeTypeName = getNodeType(moCompilationUnit);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moCompilationUnit, label);
        pushToStack(newNode);

        super.visitMoCompilationUnit(moCompilationUnit);
    }

    @Override
    public void visitMoConditionalExpression(MoConditionalExpression moConditionalExpression) {
        String nodeTypeName = getNodeType(moConditionalExpression);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moConditionalExpression, label);
        pushToStack(newNode);

        super.visitMoConditionalExpression(moConditionalExpression);
    }

    @Override
    public void visitMoConstructorInvocation(MoConstructorInvocation moConstructorInvocation) {
        String nodeTypeName = getNodeType(moConstructorInvocation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moConstructorInvocation, label);
        pushToStack(newNode);

        super.visitMoConstructorInvocation(moConstructorInvocation);
    }

    @Override
    public void visitMoContinueStatement(MoContinueStatement moContinueStatement) {
        String nodeTypeName = getNodeType(moContinueStatement);
        String label = "continue";
        Tree newNode = createNode(nodeTypeName, moContinueStatement, label);
        pushToStack(newNode);

        super.visitMoContinueStatement(moContinueStatement);
    }

    @Override
    public void visitMoDoStatement(MoDoStatement moDoStatement) {
        String nodeTypeName = getNodeType(moDoStatement);
        String label = "do";
        Tree newNode = createNode(nodeTypeName, moDoStatement, label);
        pushToStack(newNode);

        super.visitMoDoStatement(moDoStatement);
    }

    @Override
    public void visitMoEmptyStatement(MoEmptyStatement moEmptyStatement) {
        String nodeTypeName = getNodeType(moEmptyStatement);
        String label = ";";
        Tree newNode = createNode(nodeTypeName, moEmptyStatement, label);
        pushToStack(newNode);

        super.visitMoEmptyStatement(moEmptyStatement);
    }

    @Override
    public void visitMoExpressionStatement(MoExpressionStatement moExpressionStatement) {
        String nodeTypeName = getNodeType(moExpressionStatement);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moExpressionStatement, label);
        pushToStack(newNode);

        super.visitMoExpressionStatement(moExpressionStatement);
    }

    @Override
    public void visitMoFieldAccess(MoFieldAccess moFieldAccess) {
        String nodeTypeName = getNodeType(moFieldAccess);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moFieldAccess, label);
        pushToStack(newNode);

        super.visitMoFieldAccess(moFieldAccess);
    }

    @Override
    public void visitMoFieldDeclaration(MoFieldDeclaration moFieldDeclaration) {
        String nodeTypeName = getNodeType(moFieldDeclaration);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moFieldDeclaration, label);
        pushToStack(newNode);

        super.visitMoFieldDeclaration(moFieldDeclaration);
    }

    @Override
    public void visitMoForStatement(MoForStatement moForStatement) {
        String nodeTypeName = getNodeType(moForStatement);
        String label = "for";
        Tree newNode = createNode(nodeTypeName, moForStatement, label);
        pushToStack(newNode);

        super.visitMoForStatement(moForStatement);
    }

    @Override
    public void visitMoIfStatement(MoIfStatement moIfStatement) {
        String nodeTypeName = getNodeType(moIfStatement);
        String label = "if";
        Tree newNode = createNode(nodeTypeName, moIfStatement, label);
        pushToStack(newNode);

        super.visitMoIfStatement(moIfStatement);
    }

    @Override
    public void visitMoImportDeclaration(MoImportDeclaration moImportDeclaration) {
        String nodeTypeName = getNodeType(moImportDeclaration);
        String label = "import";
        Tree newNode = createNode(nodeTypeName, moImportDeclaration, label);
        pushToStack(newNode);

        super.visitMoImportDeclaration(moImportDeclaration);
    }

    @Override
    public void visitMoInfixExpression(MoInfixExpression moInfixExpression) {
        String nodeTypeName = getNodeType(moInfixExpression);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moInfixExpression, label);
        pushToStack(newNode);

        super.visitMoInfixExpression(moInfixExpression);
    }

    @Override
    public void visitMoInitializer(MoInitializer moInitializer) {
        String nodeTypeName = getNodeType(moInitializer);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moInitializer, label);
        pushToStack(newNode);

        super.visitMoInitializer(moInitializer);
    }

    @Override
    public void visitMoJavadoc(MoJavadoc moJavadoc) {
        String nodeTypeName = getNodeType(moJavadoc);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moJavadoc, label);
        pushToStack(newNode);

        super.visitMoJavadoc(moJavadoc);
    }

    @Override
    public void visitMoLabeledStatement(MoLabeledStatement moLabeledStatement) {
        String nodeTypeName = getNodeType(moLabeledStatement);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moLabeledStatement, label);
        pushToStack(newNode);

        super.visitMoLabeledStatement(moLabeledStatement);
    }

    @Override
    public void visitMoMethodDeclaration(MoMethodDeclaration moMethodDeclaration) {
        String nodeTypeName = getNodeType(moMethodDeclaration);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moMethodDeclaration, label);
        pushToStack(newNode);

        super.visitMoMethodDeclaration(moMethodDeclaration);
    }

    @Override
    public void visitMoMethodInvocation(MoMethodInvocation moMethodInvocation) {
        // 参考gen.jdt的实现, 他们专门对targetExpression和arguments进行了处理
        String nodeTypeName = getNodeType(moMethodInvocation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moMethodInvocation, label);
        pushToStack(newNode);

//        moMethodInvocation.getExpression().ifPresent(expression -> {
//            String MethodInvocationReceiverNodeTypeName = "MethodInvocationReceiver";
//            String methodInvocationReceiverLabel = "";
//            Tree receiverNode = createVirtualNode(MethodInvocationReceiverNodeTypeName, methodInvocationReceiverLabel, expression.getStartColumn(), expression.getElementLength());
//            pushToStack(receiverNode);
//            expression.accept(this);
//            stack.pop();
//        });
//
//        moMethodInvocation.getArguments().forEach(argument -> {
//            argument.accept(this);
//        });
//
//        if (!moMethodInvocation.getArguments().isEmpty()) {
//            int start = moMethodInvocation.getArguments().get(0).getStartColumn();
//            int length = moMethodInvocation.getArguments().get(moMethodInvocation.getArguments().size() - 1).getStartColumn() +
//                    moMethodInvocation.getArguments().get(moMethodInvocation.getArguments().size() - 1).getElementLength() - start;
//
//            Tree argumentsNode = createVirtualNode("MethodInvocationReceiverArguments", "", start, length);
//            pushToStack(argumentsNode);
//            moMethodInvocation.getArguments().forEach(argument -> {
//                argument.accept(this);
//            });
//            stack.pop();
//        }
//        exit(moMethodInvocation);

        super.visitMoMethodInvocation(moMethodInvocation);
    }

    @Override
    public void visitMoNullLiteral(MoNullLiteral moNullLiteral) {
        String nodeTypeName = getNodeType(moNullLiteral);
        String label = "null";
        Tree newNode = createNode(nodeTypeName, moNullLiteral, label);
        pushToStack(newNode);

        super.visitMoNullLiteral(moNullLiteral);
    }

    @Override
    public void visitMoNumberLiteral(MoNumberLiteral moNumberLiteral) {
        String nodeTypeName = getNodeType(moNumberLiteral);
        String label = moNumberLiteral.getValue();
        Tree newNode = createNode(nodeTypeName, moNumberLiteral, label);
        pushToStack(newNode);

        super.visitMoNumberLiteral(moNumberLiteral);
    }

    @Override
    public void visitMoPackageDeclaration(MoPackageDeclaration moPackageDeclaration) {
        String nodeTypeName = getNodeType(moPackageDeclaration);
        String label = "package";
        Tree newNode = createNode(nodeTypeName, moPackageDeclaration, label);
        pushToStack(newNode);

        super.visitMoPackageDeclaration(moPackageDeclaration);
    }

    @Override
    public void visitMoParenthesizedExpression(MoParenthesizedExpression moParenthesizedExpression) {
        String nodeTypeName = getNodeType(moParenthesizedExpression);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moParenthesizedExpression, label);
        pushToStack(newNode);

        super.visitMoParenthesizedExpression(moParenthesizedExpression);
    }

    @Override
    public void visitMoPostfixExpression(MoPostfixExpression moPostfixExpression) {
        String nodeTypeName = getNodeType(moPostfixExpression);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moPostfixExpression, label);
        pushToStack(newNode);

        super.visitMoPostfixExpression(moPostfixExpression);
    }

    @Override
    public void visitMoPrefixExpression(MoPrefixExpression moPrefixExpression) {
        String nodeTypeName = getNodeType(moPrefixExpression);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moPrefixExpression, label);
        pushToStack(newNode);

        super.visitMoPrefixExpression(moPrefixExpression);
    }

    @Override
    public void visitMoPrimitiveType(MoPrimitiveType moPrimitiveType) {
        String nodeTypeName = getNodeType(moPrimitiveType);
        String label = moPrimitiveType.getTypeKind().toString();
        Tree newNode = createNode(nodeTypeName, moPrimitiveType, label);
        pushToStack(newNode);

        super.visitMoPrimitiveType(moPrimitiveType);
    }

    @Override
    public void visitMoQualifiedName(MoQualifiedName moQualifiedName) {
        // 参考gen.jdt的实现, 不对QualifiedName的子节点进行处理
        String nodeTypeName = getNodeType(moQualifiedName);
        String label = moQualifiedName.toString();
        Tree newNode = createNode(nodeTypeName, moQualifiedName, label);
        pushToStack(newNode);

        exit(moQualifiedName);
    }

    @Override
    public void visitMoReturnStatement(MoReturnStatement moReturnStatement) {
        String nodeTypeName = getNodeType(moReturnStatement);
        String label = "return";
        Tree newNode = createNode(nodeTypeName, moReturnStatement, label);
        pushToStack(newNode);

        super.visitMoReturnStatement(moReturnStatement);
    }

    @Override
    public void visitMoSimpleName(MoSimpleName moSimpleName) {
        String nodeTypeName = getNodeType(moSimpleName);
        String label = moSimpleName.toString();
        Tree newNode = createNode(nodeTypeName, moSimpleName, label);
        pushToStack(newNode);

        super.visitMoSimpleName(moSimpleName);
    }

    @Override
    public void visitMoSimpleType(MoSimpleType moSimpleType) {
        String nodeTypeName = getNodeType(moSimpleType);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moSimpleType, label);
        pushToStack(newNode);

        super.visitMoSimpleType(moSimpleType);
    }

    @Override
    public void visitMoSingleVariableDeclaration(MoSingleVariableDeclaration moSingleVariableDeclaration) {
        String nodeTypeName = getNodeType(moSingleVariableDeclaration);
        String label = moSingleVariableDeclaration.isVarargs() ? "..." : "";
        Tree newNode = createNode(nodeTypeName, moSingleVariableDeclaration, label);
        pushToStack(newNode);

        super.visitMoSingleVariableDeclaration(moSingleVariableDeclaration);
    }

    @Override
    public void visitMoStringLiteral(MoStringLiteral moStringLiteral) {
        String nodeTypeName = getNodeType(moStringLiteral);
        String label = moStringLiteral.toString();
        Tree newNode = createNode(nodeTypeName, moStringLiteral, label);
        pushToStack(newNode);

        super.visitMoStringLiteral(moStringLiteral);
    }

    @Override
    public void visitMoSuperConstructorInvocation(MoSuperConstructorInvocation moSuperConstructorInvocation) {
        String nodeTypeName = getNodeType(moSuperConstructorInvocation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moSuperConstructorInvocation, label);
        pushToStack(newNode);

        super.visitMoSuperConstructorInvocation(moSuperConstructorInvocation);
    }

    @Override
    public void visitMoSuperFieldAccess(MoSuperFieldAccess moSuperFieldAccess) {
        String nodeTypeName = getNodeType(moSuperFieldAccess);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moSuperFieldAccess, label);
        pushToStack(newNode);

        super.visitMoSuperFieldAccess(moSuperFieldAccess);
    }

    @Override
    public void visitMoSuperMethodInvocation(MoSuperMethodInvocation moSuperMethodInvocation) {
        String nodeTypeName = getNodeType(moSuperMethodInvocation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moSuperMethodInvocation, label);
        pushToStack(newNode);

        super.visitMoSuperMethodInvocation(moSuperMethodInvocation);
    }

    @Override
    public void visitMoSwitchCase(MoSwitchCase moSwitchCase) {
        String nodeTypeName = getNodeType(moSwitchCase);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moSwitchCase, label);
        pushToStack(newNode);

        super.visitMoSwitchCase(moSwitchCase);
    }

    @Override
    public void visitMoSwitchStatement(MoSwitchStatement moSwitchStatement) {
        String nodeTypeName = getNodeType(moSwitchStatement);
        String label = "switch";
        Tree newNode = createNode(nodeTypeName, moSwitchStatement, label);
        pushToStack(newNode);

        super.visitMoSwitchStatement(moSwitchStatement);
    }

    @Override
    public void visitMoSynchronizedStatement(MoSynchronizedStatement moSynchronizedStatement) {
        String nodeTypeName = getNodeType(moSynchronizedStatement);
        String label = "synchronized";
        Tree newNode = createNode(nodeTypeName, moSynchronizedStatement, label);
        pushToStack(newNode);

        super.visitMoSynchronizedStatement(moSynchronizedStatement);
    }

    @Override
    public void visitMoThisExpression(MoThisExpression moThisExpression) {
        String nodeTypeName = getNodeType(moThisExpression);
        String label = "this";
        Tree newNode = createNode(nodeTypeName, moThisExpression, label);
        pushToStack(newNode);

        super.visitMoThisExpression(moThisExpression);
    }

    @Override
    public void visitMoThrowStatement(MoThrowStatement moThrowStatement) {
        String nodeTypeName = getNodeType(moThrowStatement);
        String label = "throw";
        Tree newNode = createNode(nodeTypeName, moThrowStatement, label);
        pushToStack(newNode);

        super.visitMoThrowStatement(moThrowStatement);
    }

    @Override
    public void visitMoTryStatement(MoTryStatement moTryStatement) {
        String nodeTypeName = getNodeType(moTryStatement);
        String label = "try";
        Tree newNode = createNode(nodeTypeName, moTryStatement, label);
        pushToStack(newNode);

        super.visitMoTryStatement(moTryStatement);
    }

    @Override
    public void visitMoTypeDeclaration(MoTypeDeclaration moTypeDeclaration) {
        String nodeTypeName = getNodeType(moTypeDeclaration);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moTypeDeclaration, label);
        pushToStack(newNode);

        super.visitMoTypeDeclaration(moTypeDeclaration);
    }

    @Override
    public void visitMoTypeDeclarationStatement(MoTypeDeclarationStatement moTypeDeclarationStatement) {
        String nodeTypeName = getNodeType(moTypeDeclarationStatement);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moTypeDeclarationStatement, label);
        pushToStack(newNode);

        super.visitMoTypeDeclarationStatement(moTypeDeclarationStatement);
    }

    @Override
    public void visitMoTypeLiteral(MoTypeLiteral moTypeLiteral) {
        String nodeTypeName = getNodeType(moTypeLiteral);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moTypeLiteral, label);
        pushToStack(newNode);

        super.visitMoTypeLiteral(moTypeLiteral);
    }

    @Override
    public void visitMoVariableDeclarationExpression(MoVariableDeclarationExpression moVariableDeclarationExpression) {
        String nodeTypeName = getNodeType(moVariableDeclarationExpression);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moVariableDeclarationExpression, label);
        pushToStack(newNode);

        super.visitMoVariableDeclarationExpression(moVariableDeclarationExpression);
    }

    @Override
    public void visitMoVariableDeclarationFragment(MoVariableDeclarationFragment moVariableDeclarationFragment) {
        String nodeTypeName = getNodeType(moVariableDeclarationFragment);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moVariableDeclarationFragment, label);
        pushToStack(newNode);

        super.visitMoVariableDeclarationFragment(moVariableDeclarationFragment);
    }

    @Override
    public void visitMoVariableDeclarationStatement(MoVariableDeclarationStatement moVariableDeclarationStatement) {
        String nodeTypeName = getNodeType(moVariableDeclarationStatement);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moVariableDeclarationStatement, label);
        pushToStack(newNode);

        super.visitMoVariableDeclarationStatement(moVariableDeclarationStatement);
    }

    @Override
    public void visitMoWhileStatement(MoWhileStatement moWhileStatement) {
        String nodeTypeName = getNodeType(moWhileStatement);
        String label = "while";
        Tree newNode = createNode(nodeTypeName, moWhileStatement, label);
        pushToStack(newNode);

        super.visitMoWhileStatement(moWhileStatement);
    }

    @Override
    public void visitMoInstanceofExpression(MoInstanceofExpression moInstanceofExpression) {
        String nodeTypeName = getNodeType(moInstanceofExpression);
        String label = "instanceof";
        Tree newNode = createNode(nodeTypeName, moInstanceofExpression, label);
        pushToStack(newNode);

        super.visitMoInstanceofExpression(moInstanceofExpression);
    }

    @Override
    public void visitMoLineComment(MoLineComment moLineComment) {
        String nodeTypeName = getNodeType(moLineComment);
        String label = moLineComment.toString();
        Tree newNode = createNode(nodeTypeName, moLineComment, label);
        pushToStack(newNode);

        super.visitMoLineComment(moLineComment);
    }

    @Override
    public void visitMoBlockComment(MoBlockComment moBlockComment) {
        String nodeTypeName = getNodeType(moBlockComment);
        String label = moBlockComment.toString();
        Tree newNode = createNode(nodeTypeName, moBlockComment, label);
        pushToStack(newNode);

        super.visitMoBlockComment(moBlockComment);
    }

    @Override
    public void visitMoTagElement(MoTagElement moTagElement) {
        String nodeTypeName = getNodeType(moTagElement);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moTagElement, label);
        pushToStack(newNode);

        super.visitMoTagElement(moTagElement);
    }

    @Override
    public void visitMoTextElement(MoTextElement moTextElement) {
        String nodeTypeName = getNodeType(moTextElement);
        String label = moTextElement.getText();
        Tree newNode = createNode(nodeTypeName, moTextElement, label);
        pushToStack(newNode);

        super.visitMoTextElement(moTextElement);
    }

    @Override
    public void visitMoEnhancedForStatement(MoEnhancedForStatement moEnhancedForStatement) {
        String nodeTypeName = getNodeType(moEnhancedForStatement);
        String label = "enhancedFor";
        Tree newNode = createNode(nodeTypeName, moEnhancedForStatement, label);
        pushToStack(newNode);

        super.visitMoEnhancedForStatement(moEnhancedForStatement);
    }

    @Override
    public void visitMoEnumDeclaration(MoEnumDeclaration moEnumDeclaration) {
        String nodeTypeName = getNodeType(moEnumDeclaration);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moEnumDeclaration, label);
        pushToStack(newNode);

        super.visitMoEnumDeclaration(moEnumDeclaration);
    }

    @Override
    public void visitMoEnumConstantDeclaration(MoEnumConstantDeclaration moEnumConstantDeclaration) {
        String nodeTypeName = getNodeType(moEnumConstantDeclaration);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moEnumConstantDeclaration, label);
        pushToStack(newNode);

        super.visitMoEnumConstantDeclaration(moEnumConstantDeclaration);
    }

    @Override
    public void visitMoTypeParameter(MoTypeParameter moTypeParameter) {
        String nodeTypeName = getNodeType(moTypeParameter);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moTypeParameter, label);
        pushToStack(newNode);

        super.visitMoTypeParameter(moTypeParameter);
    }

    @Override
    public void visitMoParameterizedType(MoParameterizedType moParameterizedType) {
        String nodeTypeName = getNodeType(moParameterizedType);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moParameterizedType, label);
        pushToStack(newNode);

        super.visitMoParameterizedType(moParameterizedType);
    }

    @Override
    public void visitMoQualifiedType(MoQualifiedType moQualifiedType) {
        String nodeTypeName = getNodeType(moQualifiedType);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moQualifiedType, label);
        pushToStack(newNode);

        super.visitMoQualifiedType(moQualifiedType);
    }

    @Override
    public void visitMoWildcardType(MoWildcardType moWildcardType) {
        String nodeTypeName = getNodeType(moWildcardType);
        String label = "?";
        Tree newNode = createNode(nodeTypeName, moWildcardType, label);
        pushToStack(newNode);

        super.visitMoWildcardType(moWildcardType);
    }

    @Override
    public void visitMoNormalAnnotation(MoNormalAnnotation moNormalAnnotation) {
        String nodeTypeName = getNodeType(moNormalAnnotation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moNormalAnnotation, label);
        pushToStack(newNode);

        super.visitMoNormalAnnotation(moNormalAnnotation);
    }

    @Override
    public void visitMoMarkerAnnotation(MoMarkerAnnotation moMarkerAnnotation) {
        String nodeTypeName = getNodeType(moMarkerAnnotation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moMarkerAnnotation, label);
        pushToStack(newNode);

        super.visitMoMarkerAnnotation(moMarkerAnnotation);
    }

    @Override
    public void visitMoSingleMemberAnnotation(MoSingleMemberAnnotation moSingleMemberAnnotation) {
        String nodeTypeName = getNodeType(moSingleMemberAnnotation);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moSingleMemberAnnotation, label);
        pushToStack(newNode);

        super.visitMoSingleMemberAnnotation(moSingleMemberAnnotation);
    }

    @Override
    public void visitMoMemberValuePair(MoMemberValuePair moMemberValuePair) {
        String nodeTypeName = getNodeType(moMemberValuePair);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moMemberValuePair, label);
        pushToStack(newNode);

        super.visitMoMemberValuePair(moMemberValuePair);
    }

    @Override
    public void visitMoModifier(MoModifier moModifier) {
        String nodeTypeName = getNodeType(moModifier);
        String label = moModifier.toString();
        Tree newNode = createNode(nodeTypeName, moModifier, label);
        pushToStack(newNode);

        super.visitMoModifier(moModifier);
    }

    @Override
    public void visitMoUnionType(MoUnionType moUnionType) {
        String nodeTypeName = getNodeType(moUnionType);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moUnionType, label);
        pushToStack(newNode);

        super.visitMoUnionType(moUnionType);
    }

    @Override
    public void visitMoDimension(MoDimension moDimension) {
        String nodeTypeName = getNodeType(moDimension);
        String label = "[]";
        Tree newNode = createNode(nodeTypeName, moDimension, label);
        pushToStack(newNode);

        super.visitMoDimension(moDimension);
    }

    @Override
    public void visitMoLambdaExpression(MoLambdaExpression moLambdaExpression) {
        String nodeTypeName = getNodeType(moLambdaExpression);
        String label = "->";
        Tree newNode = createNode(nodeTypeName, moLambdaExpression, label);
        pushToStack(newNode);

        super.visitMoLambdaExpression(moLambdaExpression);
    }

    @Override
    public void visitMoIntersectionType(MoIntersectionType moIntersectionType) {
        String nodeTypeName = getNodeType(moIntersectionType);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moIntersectionType, label);
        pushToStack(newNode);

        super.visitMoIntersectionType(moIntersectionType);
    }

    @Override
    public void visitMoNameQualifiedType(MoNameQualifiedType moNameQualifiedType) {
        String nodeTypeName = getNodeType(moNameQualifiedType);
        String label = moNameQualifiedType.toString();
        Tree newNode = createNode(nodeTypeName, moNameQualifiedType, label);
        pushToStack(newNode);

        super.visitMoNameQualifiedType(moNameQualifiedType);
    }

    @Override
    public void visitMoCreationReference(MoCreationReference moCreationReference) {
        String nodeTypeName = getNodeType(moCreationReference);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moCreationReference, label);
        pushToStack(newNode);

        super.visitMoCreationReference(moCreationReference);
    }

    @Override
    public void visitMoExpressionMethodReference(MoExpressionMethodReference moExpressionMethodReference) {
        String nodeTypeName = getNodeType(moExpressionMethodReference);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moExpressionMethodReference, label);
        pushToStack(newNode);

        super.visitMoExpressionMethodReference(moExpressionMethodReference);
    }

    @Override
    public void visitMoSuperMethodReference(MoSuperMethodReference moSuperMethodReference) {
        String nodeTypeName = getNodeType(moSuperMethodReference);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moSuperMethodReference, label);
        pushToStack(newNode);

        super.visitMoSuperMethodReference(moSuperMethodReference);
    }

    @Override
    public void visitMoTypeMethodReference(MoTypeMethodReference moTypeMethodReference) {
        String nodeTypeName = getNodeType(moTypeMethodReference);
        String label = "";
        Tree newNode = createNode(nodeTypeName, moTypeMethodReference, label);
        pushToStack(newNode);

        super.visitMoTypeMethodReference(moTypeMethodReference);
    }

    @Override
    public void visitMoInfixOperator(MoInfixOperator moInfixOperator) {
        String nodeTypeName = getNodeType(moInfixOperator);
        String label = "InfixOp" + moInfixOperator.getOperator().toString();
        Tree newNode = createNode(nodeTypeName, moInfixOperator, label);
        pushToStack(newNode);

        super.visitMoInfixOperator(moInfixOperator);
    }

    @Override
    public void visitMoAssignmentOperator(MoAssignmentOperator moAssignmentOperator) {
        String nodeTypeName = getNodeType(moAssignmentOperator);
        String label = "AssignOp" + moAssignmentOperator.getOperator().toString();
        Tree newNode = createNode(nodeTypeName, moAssignmentOperator, label);
        pushToStack(newNode);

        super.visitMoAssignmentOperator(moAssignmentOperator);
    }

    @Override
    public void visitMoPostfixOperator(MoPostfixOperator moPostfixOperator) {
        String nodeTypeName = getNodeType(moPostfixOperator);
        String label = "PostfixOp" + moPostfixOperator.getOperator().toString();
        Tree newNode = createNode(nodeTypeName, moPostfixOperator, label);
        pushToStack(newNode);

        super.visitMoPostfixOperator(moPostfixOperator);
    }

    @Override
    public void visitMoPrefixOperator(MoPrefixOperator moPrefixOperator) {
        String nodeTypeName = getNodeType(moPrefixOperator);
        String label = "PrefixOP" + moPrefixOperator.getOperator().toString();
        Tree newNode = createNode(nodeTypeName, moPrefixOperator, label);
        pushToStack(newNode);

        super.visitMoPrefixOperator(moPrefixOperator);
    }


    private String getNodeType(MoNode node) {
        String nodeTypeName = NOTYPE;
        if (node != null) {
            nodeTypeName = node.getClass().getSimpleName();
        } else {
            logger.error("Node should not be null");
        }
        if (node instanceof MoBlock) {
            nodeTypeName = node.getLocationInParent().role();
        }
        return nodeTypeName;
    }


    private Tree createNode(String nodeTypeName, MoNode node, String label) {
        Tree newNode = createNode(nodeTypeName, label);
        newNode.setPos(node.getStartColumn());
        newNode.setLength(node.getElementLength());
        newNode.setMetadata(GumtreeMetaConstant.MO_NODE_KEY, node);
        return newNode;
    }

    private Tree createVirtualNode(String nodeTypeName, String label, int start, int length) {
        Tree newNode = createNode(nodeTypeName, label);
        newNode.setPos(start);
        newNode.setLength(length);
        return newNode;
    }

    public Tree createNode(String typeClass, String label) {
        Type type = type(typeClass);
        return treeContext.createTree(type, label);
    }

}

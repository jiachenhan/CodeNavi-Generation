package repair.ast.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import org.eclipse.jdt.core.dom.*;
import repair.ast.MoNodeList;
import repair.ast.code.*;
import repair.ast.code.expression.*;
import repair.ast.code.expression.literal.*;
import repair.ast.code.statement.*;
import repair.ast.code.type.*;
import repair.ast.code.virtual.*;
import repair.ast.declaration.*;
import repair.ast.role.Description;

import java.util.*;


public class NodeParser extends ASTVisitor {
    private final static Logger logger = LoggerFactory.getLogger(NodeParser.class);
    private final CompilationUnit cunit;
    private final String fileName;

    public NodeParser(String fileName, CompilationUnit unit) {
        cunit = unit;
        this.fileName = fileName;
    }

    private final Deque<MoNode> nodeStack = new ArrayDeque<>();
    private MoNode rootNode = null;

    public MoNode process(ASTNode node) {
        node.accept(this);
        return rootNode;
    }



    @Override
    public boolean visit(MethodDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoMethodDeclaration method = new MoMethodDeclaration(fileName, startLine, endLine, node);
        method.setStructuralProperty("constructor", node.isConstructor());

        setPositions(method, node.getStartPosition(), node.getLength());

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, method, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = method;
        }
        nodeStack.push(method);

        return super.visit(node);
    }


    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        // 忽略注解类型声明 81
        return false;
    }

    @Override
    public boolean visit(AnnotationTypeMemberDeclaration node) {
        // 忽略注解类型成员声明 82
        return false;
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoAnonymousClassDeclaration anonymousClass = new MoAnonymousClassDeclaration(fileName, startLine, endLine, node);

        setPositions(anonymousClass, node.getStartPosition(), node.getLength());

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, anonymousClass, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = anonymousClass;
        }
        nodeStack.push(anonymousClass);

        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayAccess node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoArrayAccess arrayAccess = new MoArrayAccess(fileName, startLine, endLine, node);

        setPositions(arrayAccess, node.getStartPosition(), node.getLength());

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, arrayAccess, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = arrayAccess;
        }
        nodeStack.push(arrayAccess);

        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayCreation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoArrayCreation arrayCreation = new MoArrayCreation(fileName, startLine, endLine, node);

        setPositions(arrayCreation, node.getStartPosition(), node.getLength());

        if (!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
           bindingParentChildRelation(moParent, arrayCreation, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = arrayCreation;
        }
        nodeStack.push(arrayCreation);

        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayInitializer node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoArrayInitializer arrayInitializer = new MoArrayInitializer(fileName, startLine, endLine, node);

        setPositions(arrayInitializer, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, arrayInitializer, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = arrayInitializer;
        }
        nodeStack.push(arrayInitializer);

        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoArrayType arrayType = new MoArrayType(fileName, startLine, endLine, node);

        setPositions(arrayType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, arrayType, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = arrayType;
        }
        nodeStack.push(arrayType);

        return super.visit(node);
    }

    @Override
    public boolean visit(AssertStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoAssertStatement assertStatement = new MoAssertStatement(fileName, startLine, endLine, node);

        setPositions(assertStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, assertStatement, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = assertStatement;
        }
        nodeStack.push(assertStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoAssignment assignment = new MoAssignment(fileName, startLine, endLine, node);

        setPositions(assignment, node.getStartPosition(), node.getLength());

        // 设置operator
        MoAssignmentOperator assignmentOperator = new MoAssignmentOperator(fileName, startLine, endLine, null);
        assignmentOperator.setStructuralProperty("operator", node.getOperator().toString());
        assignment.setStructuralProperty("operator", assignmentOperator);

        // 设置description
        @SuppressWarnings("unchecked")
        Description<MoAssignment, ?> description = (Description<MoAssignment, ?>) assignment.getDescription("operator");
        assignmentOperator.setParent(assignment, description);


        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, assignment, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = assignment;
        }
        nodeStack.push(assignment);

        return super.visit(node);
    }

    @Override
    public boolean visit(Block node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoBlock block = new MoBlock(fileName, startLine, endLine, node);

        setPositions(block, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, block, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = block;
        }
        nodeStack.push(block);

        return super.visit(node);
    }

    @Override
    public boolean visit(BlockComment node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoBlockComment blockComment = new MoBlockComment(fileName, startLine, endLine, node);
        blockComment.setCommentStr(node.toString());

        setPositions(blockComment, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, blockComment, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = blockComment;
        }
        nodeStack.push(blockComment);

        return super.visit(node);
    }

    @Override
    public boolean visit(BooleanLiteral node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoBooleanLiteral booleanLiteral = new MoBooleanLiteral(fileName, startLine, endLine, node);
        booleanLiteral.setStructuralProperty("booleanValue", node.booleanValue());

        setPositions(booleanLiteral, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, booleanLiteral, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = booleanLiteral;
        }
        nodeStack.push(booleanLiteral);

        return super.visit(node);
    }

    @Override
    public boolean visit(BreakStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoBreakStatement breakStatement = new MoBreakStatement(fileName, startLine, endLine, node);

        setPositions(breakStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, breakStatement, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = breakStatement;
        }
        nodeStack.push(breakStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(CastExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoCastExpression castExpression = new MoCastExpression(fileName, startLine, endLine, node);

        setPositions(castExpression, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, castExpression, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = castExpression;
        }
        nodeStack.push(castExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(CatchClause node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoCatchClause catchClause = new MoCatchClause(fileName, startLine, endLine, node);

        setPositions(catchClause, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, catchClause, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = catchClause;
        }
        nodeStack.push(catchClause);

        return super.visit(node);
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoCharacterLiteral characterLiteral = new MoCharacterLiteral(fileName, startLine, endLine, node);
        characterLiteral.setStructuralProperty("escapedValue", node.getEscapedValue());

        setPositions(characterLiteral, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, characterLiteral, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = characterLiteral;
        }
        nodeStack.push(characterLiteral);

        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoClassInstanceCreation classInstanceCreation = new MoClassInstanceCreation(fileName, startLine, endLine, node);
        classInstanceCreation.setTypeInferred(node.isResolvedTypeInferredFromExpectedType());

        setPositions(classInstanceCreation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, classInstanceCreation, node);
        } else {
            // 如果nodeStack为空，说明是根节点
            rootNode = classInstanceCreation;
        }
        nodeStack.push(classInstanceCreation);

        return super.visit(node);
    }

    @Override
    public boolean visit(CompilationUnit node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoCompilationUnit compilationUnit = new MoCompilationUnit(fileName, startLine, endLine, node);

        setPositions(compilationUnit, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            logger.error("CompilationUnit should be the root node.");
        } else {
            rootNode = compilationUnit;
        }
        nodeStack.push(compilationUnit);

        return super.visit(node);
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoConditionalExpression conditionalExpression = new MoConditionalExpression(fileName, startLine, endLine, node);

        setPositions(conditionalExpression, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, conditionalExpression, node);
        } else {
            rootNode = conditionalExpression;
        }
        nodeStack.push(conditionalExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoConstructorInvocation constructorInvocation = new MoConstructorInvocation(fileName, startLine, endLine, node);

        setPositions(constructorInvocation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, constructorInvocation, node);
        } else {
            rootNode = constructorInvocation;
        }
        nodeStack.push(constructorInvocation);

        return super.visit(node);
    }

    @Override
    public boolean visit(ContinueStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoContinueStatement continueStatement = new MoContinueStatement(fileName, startLine, endLine, node);

        setPositions(continueStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, continueStatement, node);
        } else {
            rootNode = continueStatement;
        }
        nodeStack.push(continueStatement);


        return super.visit(node);
    }

    @Override
    public boolean visit(CreationReference node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoCreationReference creationReference = new MoCreationReference(fileName, startLine, endLine, node);

        setPositions(creationReference, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, creationReference, node);
        } else {
            rootNode = creationReference;
        }
        nodeStack.push(creationReference);

        return super.visit(node);
    }

    @Override
    public boolean visit(Dimension node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoDimension dimension = new MoDimension(fileName, startLine, endLine, node);

        setPositions(dimension, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, dimension, node);
        } else {
            rootNode = dimension;
        }
        nodeStack.push(dimension);

        return super.visit(node);
    }

    @Override
    public boolean visit(DoStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoDoStatement doStatement = new MoDoStatement(fileName, startLine, endLine, node);

        setPositions(doStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, doStatement, node);
        } else {
            rootNode = doStatement;
        }
        nodeStack.push(doStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(EmptyStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoEmptyStatement emptyStatement = new MoEmptyStatement(fileName, startLine, endLine, node);

        setPositions(emptyStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, emptyStatement, node);
        } else {
            rootNode = emptyStatement;
        }
        nodeStack.push(emptyStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoEnhancedForStatement enhancedForStatement = new MoEnhancedForStatement(fileName, startLine, endLine, node);

        setPositions(enhancedForStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, enhancedForStatement, node);
        } else {
            rootNode = enhancedForStatement;
        }
        nodeStack.push(enhancedForStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(EnumConstantDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoEnumConstantDeclaration enumConstantDeclaration = new MoEnumConstantDeclaration(fileName, startLine, endLine, node);

        setPositions(enumConstantDeclaration, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, enumConstantDeclaration, node);
        } else {
            rootNode = enumConstantDeclaration;
        }
        nodeStack.push(enumConstantDeclaration);

        return super.visit(node);
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoEnumDeclaration enumDeclaration = new MoEnumDeclaration(fileName, startLine, endLine, node);

        setPositions(enumDeclaration, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, enumDeclaration, node);
        } else {
            rootNode = enumDeclaration;
        }
        nodeStack.push(enumDeclaration);

        return super.visit(node);
    }

    @Override
    public boolean visit(ExpressionMethodReference node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoExpressionMethodReference expressionMethodReference = new MoExpressionMethodReference(fileName, startLine, endLine, node);

        setPositions(expressionMethodReference, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, expressionMethodReference, node);
        } else {
            rootNode = expressionMethodReference;
        }
        nodeStack.push(expressionMethodReference);

        return super.visit(node);
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoExpressionStatement expressionStatement = new MoExpressionStatement(fileName, startLine, endLine, node);

        setPositions(expressionStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, expressionStatement, node);
        } else {
            rootNode = expressionStatement;
        }
        nodeStack.push(expressionStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(FieldAccess node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoFieldAccess fieldAccess = new MoFieldAccess(fileName, startLine, endLine, node);

        setPositions(fieldAccess, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, fieldAccess, node);
        } else {
            rootNode = fieldAccess;
        }
        nodeStack.push(fieldAccess);

        return super.visit(node);
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoFieldDeclaration fieldDeclaration = new MoFieldDeclaration(fileName, startLine, endLine, node);

        setPositions(fieldDeclaration, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, fieldDeclaration, node);
        } else {
            rootNode = fieldDeclaration;
        }
        nodeStack.push(fieldDeclaration);

        return super.visit(node);
    }

    @Override
    public boolean visit(ForStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoForStatement forStatement = new MoForStatement(fileName, startLine, endLine, node);

        setPositions(forStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, forStatement, node);
        } else {
            rootNode = forStatement;
        }
        nodeStack.push(forStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(IfStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoIfStatement ifStatement = new MoIfStatement(fileName, startLine, endLine, node);

        setPositions(ifStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, ifStatement, node);
        } else {
            rootNode = ifStatement;
        }
        nodeStack.push(ifStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoImportDeclaration importDeclaration = new MoImportDeclaration(fileName, startLine, endLine, node);
        importDeclaration.setStructuralProperty("static", node.isStatic());
        importDeclaration.setStructuralProperty("onDemand", node.isOnDemand());

        setPositions(importDeclaration, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, importDeclaration, node);
        } else {
            rootNode = importDeclaration;
        }
        nodeStack.push(importDeclaration);

        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoInfixExpression infixExpression = new MoInfixExpression(fileName, startLine, endLine, node);

        setPositions(infixExpression, node.getStartPosition(), node.getLength());

        // 设置operator
        MoInfixOperator infixOperator = new MoInfixOperator(fileName, startLine, endLine, null);
        infixOperator.setStructuralProperty("operator", node.getOperator().toString());
        infixExpression.setStructuralProperty("operator", infixOperator);

        // 设置description
        @SuppressWarnings("unchecked")
        Description<MoInfixExpression, ?> description = (Description<MoInfixExpression, ?>) infixExpression.getDescription("operator");
        infixOperator.setParent(infixExpression, description);

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            // 设置parent父子关系
            bindingParentChildRelation(moParent, infixExpression, node);
        } else {
            rootNode = infixExpression;
        }
        nodeStack.push(infixExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(Initializer node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoInitializer initializer = new MoInitializer(fileName, startLine, endLine, node);

        setPositions(initializer, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, initializer, node);
        } else {
            rootNode = initializer;
        }
        nodeStack.push(initializer);

        return super.visit(node);
    }

    @Override
    public boolean visit(InstanceofExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoInstanceofExpression instanceofExpression = new MoInstanceofExpression(fileName, startLine, endLine, node);

        setPositions(instanceofExpression, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, instanceofExpression, node);
        } else {
            rootNode = instanceofExpression;
        }
        nodeStack.push(instanceofExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(IntersectionType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoIntersectionType intersectionType = new MoIntersectionType(fileName, startLine, endLine, node);

        setPositions(intersectionType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, intersectionType, node);
        } else {
            rootNode = intersectionType;
        }
        nodeStack.push(intersectionType);

        return super.visit(node);
    }

    @Override
    public boolean visit(Javadoc node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoJavadoc javadoc = new MoJavadoc(fileName, startLine, endLine, node);

        setPositions(javadoc, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, javadoc, node);
        } else {
            rootNode = javadoc;
        }
        nodeStack.push(javadoc);

        return super.visit(node);
    }

    @Override
    public boolean visit(LabeledStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoLabeledStatement labeledStatement = new MoLabeledStatement(fileName, startLine, endLine, node);

        setPositions(labeledStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, labeledStatement, node);
        } else {
            rootNode = labeledStatement;
        }
        nodeStack.push(labeledStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(LambdaExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoLambdaExpression lambdaExpression = new MoLambdaExpression(fileName, startLine, endLine, node);
        lambdaExpression.setStructuralProperty("parentheses", node.hasParentheses());

        setPositions(lambdaExpression, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, lambdaExpression, node);
        } else {
            rootNode = lambdaExpression;
        }
        nodeStack.push(lambdaExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(LineComment node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoLineComment lineComment = new MoLineComment(fileName, startLine, endLine, node);
        lineComment.setCommentStr(node.toString());

        setPositions(lineComment, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, lineComment, node);
        } else {
            rootNode = lineComment;
        }
        nodeStack.push(lineComment);

        return super.visit(node);
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoMarkerAnnotation markerAnnotation = new MoMarkerAnnotation(fileName, startLine, endLine, node);

        setPositions(markerAnnotation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, markerAnnotation, node);
        } else {
            rootNode = markerAnnotation;
        }
        nodeStack.push(markerAnnotation);

        return super.visit(node);
    }

    @Override
    public boolean visit(MemberRef node) {
        // 忽略成员引用(javadoc) 67
        return false;
    }

    @Override
    public boolean visit(MemberValuePair node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoMemberValuePair memberValuePair = new MoMemberValuePair(fileName, startLine, endLine, node);

        setPositions(memberValuePair, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, memberValuePair, node);
        } else {
            rootNode = memberValuePair;
        }
        nodeStack.push(memberValuePair);

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodRef node) {
        // 忽略方法引用(javadoc) 68
        return false;
    }

    @Override
    public boolean visit(MethodRefParameter node) {
        // 忽略方法引用参数(javadoc) 69
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoMethodInvocation methodInvocation = new MoMethodInvocation(fileName, startLine, endLine, node);
        methodInvocation.setTypeInferred(node.isResolvedTypeInferredFromExpectedType());

        setPositions(methodInvocation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, methodInvocation, node);
        } else {
            rootNode = methodInvocation;
        }
        nodeStack.push(methodInvocation);


        if (node.getExpression() == null) {
            methodInvocation.setStructuralProperty("expression", null);
        } else {
            MoMethodInvocationTarget methodInvocationTarget = new MoMethodInvocationTarget(fileName, startLine, endLine, null);
            methodInvocation.setStructuralProperty("expression", methodInvocationTarget);

            methodInvocationTarget.setParent(methodInvocation, methodInvocation.getDescription("expression"));

            nodeStack.push(methodInvocationTarget);
            node.getExpression().accept(this);
            nodeStack.pop();
        }

        node.getName().accept(this);

        for (Object typeArgument : node.typeArguments()) {
            if(typeArgument instanceof Type type) {
                type.accept(this);
            } else {
                logger.error("MethodInvocation typeArgument is not Type.");
            }
        }

        if(!node.arguments().isEmpty()){
            MoMethodInvocationArguments methodInvocationArguments = new MoMethodInvocationArguments(fileName, startLine, endLine, null);
            methodInvocation.setStructuralProperty("arguments", methodInvocationArguments);

            methodInvocationArguments.setParent(methodInvocation, methodInvocation.getDescription("arguments"));

            nodeStack.push(methodInvocationArguments);
            for (Object argument : node.arguments()) {
                if(argument instanceof Expression expression) {
                    expression.accept(this);
                } else {
                    logger.error("MethodInvocation argument is not Expression.");
                }
            }
            nodeStack.pop();
        }
        return false;
    }

    @Override
    public boolean visit(Modifier node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoModifier modifier = new MoModifier(fileName, startLine, endLine, node);
        modifier.setStructuralProperty("keyword", node.getKeyword().toString());

        setPositions(modifier, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, modifier, node);
        } else {
            rootNode = modifier;
        }
        nodeStack.push(modifier);

        return super.visit(node);
    }

    @Override
    public boolean visit(NameQualifiedType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoNameQualifiedType nameQualifiedType = new MoNameQualifiedType(fileName, startLine, endLine, node);

        setPositions(nameQualifiedType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, nameQualifiedType, node);
        } else {
            rootNode = nameQualifiedType;
        }
        nodeStack.push(nameQualifiedType);

        return super.visit(node);
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoNormalAnnotation normalAnnotation = new MoNormalAnnotation(fileName, startLine, endLine, node);

        setPositions(normalAnnotation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, normalAnnotation, node);
        } else {
            rootNode = normalAnnotation;
        }
        nodeStack.push(normalAnnotation);

        return super.visit(node);
    }

    @Override
    public boolean visit(NullLiteral node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoNullLiteral nullLiteral = new MoNullLiteral(fileName, startLine, endLine, node);

        setPositions(nullLiteral, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, nullLiteral, node);
        } else {
            rootNode = nullLiteral;
        }
        nodeStack.push(nullLiteral);

        return super.visit(node);
    }

    @Override
    public boolean visit(NumberLiteral node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoNumberLiteral numberLiteral = new MoNumberLiteral(fileName, startLine, endLine, node);
        numberLiteral.setStructuralProperty("token", node.getToken());

        setPositions(numberLiteral, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, numberLiteral, node);
        } else {
            rootNode = numberLiteral;
        }
        nodeStack.push(numberLiteral);

        return super.visit(node);
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoPackageDeclaration packageDeclaration = new MoPackageDeclaration(fileName, startLine, endLine, node);

        setPositions(packageDeclaration, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, packageDeclaration, node);
        } else {
            rootNode = packageDeclaration;
        }
        nodeStack.push(packageDeclaration);

        return super.visit(node);
    }

    @Override
    public boolean visit(ParameterizedType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoParameterizedType parameterizedType = new MoParameterizedType(fileName, startLine, endLine, node);

        setPositions(parameterizedType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, parameterizedType, node);
        } else {
            rootNode = parameterizedType;
        }
        nodeStack.push(parameterizedType);

        return super.visit(node);
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoParenthesizedExpression parenthesizedExpression = new MoParenthesizedExpression(fileName, startLine, endLine, node);

        setPositions(parenthesizedExpression, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, parenthesizedExpression, node);
        } else {
            rootNode = parenthesizedExpression;
        }
        nodeStack.push(parenthesizedExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(PostfixExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoPostfixExpression postfixExpression = new MoPostfixExpression(fileName, startLine, endLine, node);

        setPositions(postfixExpression, node.getStartPosition(), node.getLength());

        // 设置operator
        MoPostfixOperator postfixOperator = new MoPostfixOperator(fileName, startLine, endLine, null);
        postfixOperator.setStructuralProperty("operator", node.getOperator().toString());
        postfixExpression.setStructuralProperty("operator", postfixOperator);

        // 设置description
        @SuppressWarnings("unchecked")
        Description<MoPostfixExpression, ?> description = (Description<MoPostfixExpression, ?>) postfixExpression.getDescription("operator");
        postfixOperator.setParent(postfixExpression, description);

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, postfixExpression, node);
        } else {
            rootNode = postfixExpression;
        }
        nodeStack.push(postfixExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(PrefixExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoPrefixExpression prefixExpression = new MoPrefixExpression(fileName, startLine, endLine, node);

        setPositions(prefixExpression, node.getStartPosition(), node.getLength());

        // 设置operator
        MoPrefixOperator prefixOperator = new MoPrefixOperator(fileName, startLine, endLine, null);
        prefixOperator.setStructuralProperty("operator", node.getOperator().toString());
        prefixExpression.setStructuralProperty("operator", prefixOperator);

        // 设置description
        @SuppressWarnings("unchecked")
        Description<MoPrefixExpression, ?> description = (Description<MoPrefixExpression, ?>) prefixExpression.getDescription("operator");
        prefixOperator.setParent(prefixExpression, description);

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, prefixExpression, node);
        } else {
            rootNode = prefixExpression;
        }
        nodeStack.push(prefixExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(PrimitiveType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoPrimitiveType primitiveType = new MoPrimitiveType(fileName, startLine, endLine, node);
        primitiveType.setStructuralProperty("primitiveTypeCode", node.getPrimitiveTypeCode().toString());

        setPositions(primitiveType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, primitiveType, node);
        } else {
            rootNode = primitiveType;
        }
        nodeStack.push(primitiveType);

        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoQualifiedName qualifiedName = new MoQualifiedName(fileName, startLine, endLine, node);

        setPositions(qualifiedName, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, qualifiedName, node);
        } else {
            rootNode = qualifiedName;
        }
        nodeStack.push(qualifiedName);

        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoQualifiedType qualifiedType = new MoQualifiedType(fileName, startLine, endLine, node);

        setPositions(qualifiedType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, qualifiedType, node);
        } else {
            rootNode = qualifiedType;
        }
        nodeStack.push(qualifiedType);

        return super.visit(node);
    }

    @Override
    public boolean visit(ReturnStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoReturnStatement returnStatement = new MoReturnStatement(fileName, startLine, endLine, node);

        setPositions(returnStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, returnStatement, node);
        } else {
            rootNode = returnStatement;
        }
        nodeStack.push(returnStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleName node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSimpleName simpleName = new MoSimpleName(fileName, startLine, endLine, node);
        simpleName.setStructuralProperty("identifier", node.getIdentifier());

        setPositions(simpleName, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, simpleName, node);
        } else {
            rootNode = simpleName;
        }
        nodeStack.push(simpleName);

        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSimpleType simpleType = new MoSimpleType(fileName, startLine, endLine, node);

        setPositions(simpleType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, simpleType, node);
        } else {
            rootNode = simpleType;
        }
        nodeStack.push(simpleType);

        return super.visit(node);
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSingleMemberAnnotation singleMemberAnnotation = new MoSingleMemberAnnotation(fileName, startLine, endLine, node);

        setPositions(singleMemberAnnotation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, singleMemberAnnotation, node);
        } else {
            rootNode = singleMemberAnnotation;
        }
        nodeStack.push(singleMemberAnnotation);

        return super.visit(node);
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSingleVariableDeclaration singleVariableDeclaration = new MoSingleVariableDeclaration(fileName, startLine, endLine, node);
        singleVariableDeclaration.setStructuralProperty("varargs", node.isVarargs());

        setPositions(singleVariableDeclaration, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, singleVariableDeclaration, node);
        } else {
            rootNode = singleVariableDeclaration;
        }
        nodeStack.push(singleVariableDeclaration);

        return super.visit(node);
    }

    @Override
    public boolean visit(StringLiteral node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoStringLiteral stringLiteral = new MoStringLiteral(fileName, startLine, endLine, node);
        stringLiteral.setStructuralProperty("escapedValue", node.getEscapedValue());

        setPositions(stringLiteral, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, stringLiteral, node);
        } else {
            rootNode = stringLiteral;
        }
        nodeStack.push(stringLiteral);

        return super.visit(node);
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSuperConstructorInvocation superConstructorInvocation = new MoSuperConstructorInvocation(fileName, startLine, endLine, node);

        setPositions(superConstructorInvocation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, superConstructorInvocation, node);
        } else {
            rootNode = superConstructorInvocation;
        }
        nodeStack.push(superConstructorInvocation);

        return super.visit(node);
    }

    @Override
    public boolean visit(SuperFieldAccess node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSuperFieldAccess superFieldAccess = new MoSuperFieldAccess(fileName, startLine, endLine, node);

        setPositions(superFieldAccess, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, superFieldAccess, node);
        } else {
            rootNode = superFieldAccess;
        }
        nodeStack.push(superFieldAccess);

        return super.visit(node);
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSuperMethodInvocation superMethodInvocation = new MoSuperMethodInvocation(fileName, startLine, endLine, node);
        superMethodInvocation.setTypeInferred(node.isResolvedTypeInferredFromExpectedType());

        setPositions(superMethodInvocation, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, superMethodInvocation, node);
        } else {
            rootNode = superMethodInvocation;
        }
        nodeStack.push(superMethodInvocation);

        return super.visit(node);
    }

    @Override
    public boolean visit(SuperMethodReference node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSuperMethodReference superMethodReference = new MoSuperMethodReference(fileName, startLine, endLine, node);

        setPositions(superMethodReference, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, superMethodReference, node);
        } else {
            rootNode = superMethodReference;
        }
        nodeStack.push(superMethodReference);

        return super.visit(node);
    }

    @Override
    public boolean visit(SwitchCase node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSwitchCase switchCase = new MoSwitchCase(fileName, startLine, endLine, node);

        setPositions(switchCase, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, switchCase, node);
        } else {
            rootNode = switchCase;
        }
        nodeStack.push(switchCase);

        return super.visit(node);
    }

    @Override
    public boolean visit(SwitchStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSwitchStatement switchStatement = new MoSwitchStatement(fileName, startLine, endLine, node);

        setPositions(switchStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, switchStatement, node);
        } else {
            rootNode = switchStatement;
        }
        nodeStack.push(switchStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoSynchronizedStatement synchronizedStatement = new MoSynchronizedStatement(fileName, startLine, endLine, node);

        setPositions(synchronizedStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, synchronizedStatement, node);
        } else {
            rootNode = synchronizedStatement;
        }
        nodeStack.push(synchronizedStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(TagElement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTagElement tagElement = new MoTagElement(fileName, startLine, endLine, node);
        tagElement.setStructuralProperty("tagName", node.getTagName());

        setPositions(tagElement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, tagElement, node);
        } else {
            rootNode = tagElement;
        }
        nodeStack.push(tagElement);

        return super.visit(node);
    }

    @Override
    public boolean visit(TextElement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTextElement textElement = new MoTextElement(fileName, startLine, endLine, node);
        textElement.setStructuralProperty("text", node.getText());

        setPositions(textElement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, textElement, node);
        } else {
            rootNode = textElement;
        }
        nodeStack.push(textElement);

        return super.visit(node);
    }

    @Override
    public boolean visit(ThisExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoThisExpression thisExpression = new MoThisExpression(fileName, startLine, endLine, node);

        setPositions(thisExpression, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, thisExpression, node);
        } else {
            rootNode = thisExpression;
        }
        nodeStack.push(thisExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(ThrowStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoThrowStatement throwStatement = new MoThrowStatement(fileName, startLine, endLine, node);

        setPositions(throwStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, throwStatement, node);
        } else {
            rootNode = throwStatement;
        }
        nodeStack.push(throwStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(TryStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTryStatement tryStatement = new MoTryStatement(fileName, startLine, endLine, node);

        setPositions(tryStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, tryStatement, node);
        } else {
            rootNode = tryStatement;
        }
        nodeStack.push(tryStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTypeDeclaration typeDeclaration = new MoTypeDeclaration(fileName, startLine, endLine, node);
        typeDeclaration.setStructuralProperty("interface", node.isInterface());

        setPositions(typeDeclaration, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, typeDeclaration, node);
        } else {
            rootNode = typeDeclaration;
        }
        nodeStack.push(typeDeclaration);

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTypeDeclarationStatement typeDeclarationStatement = new MoTypeDeclarationStatement(fileName, startLine, endLine, node);

        setPositions(typeDeclarationStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, typeDeclarationStatement, node);
        } else {
            rootNode = typeDeclarationStatement;
        }
        nodeStack.push(typeDeclarationStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeLiteral node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTypeLiteral typeLiteral = new MoTypeLiteral(fileName, startLine, endLine, node);

        setPositions(typeLiteral, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, typeLiteral, node);
        } else {
            rootNode = typeLiteral;
        }
        nodeStack.push(typeLiteral);

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTypeMethodReference typeMethodReference = new MoTypeMethodReference(fileName, startLine, endLine, node);

        setPositions(typeMethodReference, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, typeMethodReference, node);
        } else {
            rootNode = typeMethodReference;
        }
        nodeStack.push(typeMethodReference);

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeParameter node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoTypeParameter typeParameter = new MoTypeParameter(fileName, startLine, endLine, node);

        setPositions(typeParameter, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, typeParameter, node);
        } else {
            rootNode = typeParameter;
        }
        nodeStack.push(typeParameter);

        return super.visit(node);
    }

    @Override
    public boolean visit(UnionType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoUnionType unionType = new MoUnionType(fileName, startLine, endLine, node);

        setPositions(unionType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, unionType, node);
        } else {
            rootNode = unionType;
        }
        nodeStack.push(unionType);

        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoVariableDeclarationExpression variableDeclarationExpression = new MoVariableDeclarationExpression(fileName, startLine, endLine, node);

        setPositions(variableDeclarationExpression, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, variableDeclarationExpression, node);
        } else {
            rootNode = variableDeclarationExpression;
        }
        nodeStack.push(variableDeclarationExpression);

        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoVariableDeclarationStatement variableDeclarationStatement = new MoVariableDeclarationStatement(fileName, startLine, endLine, node);

        setPositions(variableDeclarationStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, variableDeclarationStatement, node);
        } else {
            rootNode = variableDeclarationStatement;
        }
        nodeStack.push(variableDeclarationStatement);


        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoVariableDeclarationFragment variableDeclarationFragment = new MoVariableDeclarationFragment(fileName, startLine, endLine, node);

        setPositions(variableDeclarationFragment, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, variableDeclarationFragment, node);
        } else {
            rootNode = variableDeclarationFragment;
        }
        nodeStack.push(variableDeclarationFragment);

        return super.visit(node);
    }

    @Override
    public boolean visit(WhileStatement node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoWhileStatement whileStatement = new MoWhileStatement(fileName, startLine, endLine, node);

        setPositions(whileStatement, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, whileStatement, node);
        } else {
            rootNode = whileStatement;
        }
        nodeStack.push(whileStatement);

        return super.visit(node);
    }

    @Override
    public boolean visit(WildcardType node) {
        int startLine = getStartLine(node);
        int endLine = getEndLine(node);
        MoWildcardType wildcardType = new MoWildcardType(fileName, startLine, endLine, node);
        wildcardType.setStructuralProperty("upperBound", node.isUpperBound());

        setPositions(wildcardType, node.getStartPosition(), node.getLength());

        if(!nodeStack.isEmpty()) {
            MoNode moParent = nodeStack.peek();
            bindingParentChildRelation(moParent, wildcardType, node);
        } else {
            rootNode = wildcardType;
        }
        nodeStack.push(wildcardType);

        return super.visit(node);
    }



    public void endVisit(MethodDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(AnnotationTypeDeclaration node) {
        // 忽略注解类型声明 81
        super.endVisit(node);
    }

    @Override
    public void endVisit(AnnotationTypeMemberDeclaration node) {
        // 忽略注解类型成员声明 82
        super.endVisit(node);
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ArrayAccess node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ArrayCreation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ArrayInitializer node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ArrayType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(AssertStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(Assignment node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(Block node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(BlockComment node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(BooleanLiteral node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(BreakStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(CastExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(CatchClause node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(CharacterLiteral node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ClassInstanceCreation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(CompilationUnit node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ConditionalExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ConstructorInvocation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ContinueStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(CreationReference node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(DoStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(EmptyStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(EnhancedForStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(EnumConstantDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(EnumDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ExpressionMethodReference node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ExpressionStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(Dimension node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(FieldAccess node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(FieldDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ForStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(IfStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ImportDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(InfixExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(InstanceofExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(Initializer node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(Javadoc node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(LabeledStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(LambdaExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(LineComment node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(MarkerAnnotation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(MemberRef node) {
        // 忽略成员引用(javadoc) 67
        super.endVisit(node);
    }

    @Override
    public void endVisit(MemberValuePair node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(MethodRef node) {
        // 忽略方法引用(javadoc) 68
        super.endVisit(node);
    }

    @Override
    public void endVisit(MethodRefParameter node) {
        // 忽略方法引用参数(javadoc) 69
        super.endVisit(node);
    }

    @Override
    public void endVisit(MethodInvocation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(Modifier node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(NameQualifiedType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(NormalAnnotation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(NullLiteral node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(NumberLiteral node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(PackageDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ParameterizedType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ParenthesizedExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(PostfixExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(PrefixExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(PrimitiveType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(QualifiedName node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(QualifiedType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ReturnStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SimpleName node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SimpleType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SingleMemberAnnotation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SingleVariableDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(StringLiteral node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SuperConstructorInvocation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SuperFieldAccess node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SuperMethodInvocation node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SuperMethodReference node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SwitchCase node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SwitchStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(SynchronizedStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TagElement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TextElement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ThisExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(ThrowStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TryStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TypeDeclarationStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TypeLiteral node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TypeMethodReference node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(TypeParameter node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(UnionType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(IntersectionType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(VariableDeclarationExpression node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(VariableDeclarationStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(VariableDeclarationFragment node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(WhileStatement node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    @Override
    public void endVisit(WildcardType node) {
        nodeStack.pop();
        super.endVisit(node);
    }

    /**
     * 内部函数，用于绑定新节点的父子关系
     * @param moParent Mo父节点
     * @param moChild Mo子节点
     * @param oriChild 原始AST子节点
     */
    private void bindingParentChildRelation(MoNode moParent, MoNode moChild, ASTNode oriChild) {
        StructuralPropertyDescriptor property = oriChild.getLocationInParent();
        Description<? extends MoNode, ?> description = moParent.getDescription(property.getId());
        assert description != null;
        moChild.setParent(moParent, description);
        if (property instanceof ChildListPropertyDescriptor listPropertyDescriptor) {
            if (isSubclassOrSameClass(listPropertyDescriptor.getElementType(), oriChild.getClass())) {
                moParent.addStructuralPropertyList(property.getId(), moChild);
            } else {
                logger.error("childList element type is not {} in {}", moChild.getClass(), moParent);
            }
        } else if(property instanceof ChildPropertyDescriptor childPropertyDescriptor) {
            if (isSubclassOrSameClass(childPropertyDescriptor.getChildType(), oriChild.getClass())) {
                moParent.setStructuralProperty(property.getId(), moChild);
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

    private int getStartLine(ASTNode node) {
        return cunit.getLineNumber(node.getStartPosition());
    }

    private int getEndLine(ASTNode node) {
        return cunit.getLineNumber(node.getStartPosition() + node.getLength());
    }

    private void setPositions(MoNode moNode, int startPosition, int length) {
        moNode.setStartColumn(startPosition);
        moNode.setElementLength(length);
    }

}


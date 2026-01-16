# Generated from DSL.g4 by ANTLR 4.13.0
from antlr4 import *
if "." in __name__:
    from .DSLParser import DSLParser
else:
    from DSLParser import DSLParser

# This class defines a complete generic visitor for a parse tree produced by DSLParser.

class DSLVisitor(ParseTreeVisitor):

    # Visit a parse tree produced by DSLParser#query.
    def visitQuery(self, ctx:DSLParser.QueryContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#nestedQuery.
    def visitNestedQuery(self, ctx:DSLParser.NestedQueryContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#entityDecl.
    def visitEntityDecl(self, ctx:DSLParser.EntityDeclContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#nodeType.
    def visitNodeType(self, ctx:DSLParser.NodeTypeContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#alias.
    def visitAlias(self, ctx:DSLParser.AliasContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#condition.
    def visitCondition(self, ctx:DSLParser.ConditionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#atomicCondition.
    def visitAtomicCondition(self, ctx:DSLParser.AtomicConditionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#valueMatch.
    def visitValueMatch(self, ctx:DSLParser.ValueMatchContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#attributeOnly.
    def visitAttributeOnly(self, ctx:DSLParser.AttributeOnlyContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#relMatch.
    def visitRelMatch(self, ctx:DSLParser.RelMatchContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#attribute.
    def visitAttribute(self, ctx:DSLParser.AttributeContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#propertyName.
    def visitPropertyName(self, ctx:DSLParser.PropertyNameContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#value.
    def visitValue(self, ctx:DSLParser.ValueContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#identifier.
    def visitIdentifier(self, ctx:DSLParser.IdentifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#stringLiteral.
    def visitStringLiteral(self, ctx:DSLParser.StringLiteralContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#numberLiteral.
    def visitNumberLiteral(self, ctx:DSLParser.NumberLiteralContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#booleanLiteral.
    def visitBooleanLiteral(self, ctx:DSLParser.BooleanLiteralContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#valueComp.
    def visitValueComp(self, ctx:DSLParser.ValueCompContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by DSLParser#nodeComp.
    def visitNodeComp(self, ctx:DSLParser.NodeCompContext):
        return self.visitChildren(ctx)



del DSLParser
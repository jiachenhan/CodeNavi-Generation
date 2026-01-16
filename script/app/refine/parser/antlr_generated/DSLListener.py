# Generated from DSL.g4 by ANTLR 4.13.0
from antlr4 import *
if "." in __name__:
    from .DSLParser import DSLParser
else:
    from DSLParser import DSLParser

# This class defines a complete listener for a parse tree produced by DSLParser.
class DSLListener(ParseTreeListener):

    # Enter a parse tree produced by DSLParser#query.
    def enterQuery(self, ctx:DSLParser.QueryContext):
        pass

    # Exit a parse tree produced by DSLParser#query.
    def exitQuery(self, ctx:DSLParser.QueryContext):
        pass


    # Enter a parse tree produced by DSLParser#nestedQuery.
    def enterNestedQuery(self, ctx:DSLParser.NestedQueryContext):
        pass

    # Exit a parse tree produced by DSLParser#nestedQuery.
    def exitNestedQuery(self, ctx:DSLParser.NestedQueryContext):
        pass


    # Enter a parse tree produced by DSLParser#entityDecl.
    def enterEntityDecl(self, ctx:DSLParser.EntityDeclContext):
        pass

    # Exit a parse tree produced by DSLParser#entityDecl.
    def exitEntityDecl(self, ctx:DSLParser.EntityDeclContext):
        pass


    # Enter a parse tree produced by DSLParser#nodeType.
    def enterNodeType(self, ctx:DSLParser.NodeTypeContext):
        pass

    # Exit a parse tree produced by DSLParser#nodeType.
    def exitNodeType(self, ctx:DSLParser.NodeTypeContext):
        pass


    # Enter a parse tree produced by DSLParser#alias.
    def enterAlias(self, ctx:DSLParser.AliasContext):
        pass

    # Exit a parse tree produced by DSLParser#alias.
    def exitAlias(self, ctx:DSLParser.AliasContext):
        pass


    # Enter a parse tree produced by DSLParser#condition.
    def enterCondition(self, ctx:DSLParser.ConditionContext):
        pass

    # Exit a parse tree produced by DSLParser#condition.
    def exitCondition(self, ctx:DSLParser.ConditionContext):
        pass


    # Enter a parse tree produced by DSLParser#atomicCondition.
    def enterAtomicCondition(self, ctx:DSLParser.AtomicConditionContext):
        pass

    # Exit a parse tree produced by DSLParser#atomicCondition.
    def exitAtomicCondition(self, ctx:DSLParser.AtomicConditionContext):
        pass


    # Enter a parse tree produced by DSLParser#valueMatch.
    def enterValueMatch(self, ctx:DSLParser.ValueMatchContext):
        pass

    # Exit a parse tree produced by DSLParser#valueMatch.
    def exitValueMatch(self, ctx:DSLParser.ValueMatchContext):
        pass


    # Enter a parse tree produced by DSLParser#attributeOnly.
    def enterAttributeOnly(self, ctx:DSLParser.AttributeOnlyContext):
        pass

    # Exit a parse tree produced by DSLParser#attributeOnly.
    def exitAttributeOnly(self, ctx:DSLParser.AttributeOnlyContext):
        pass


    # Enter a parse tree produced by DSLParser#relMatch.
    def enterRelMatch(self, ctx:DSLParser.RelMatchContext):
        pass

    # Exit a parse tree produced by DSLParser#relMatch.
    def exitRelMatch(self, ctx:DSLParser.RelMatchContext):
        pass


    # Enter a parse tree produced by DSLParser#attribute.
    def enterAttribute(self, ctx:DSLParser.AttributeContext):
        pass

    # Exit a parse tree produced by DSLParser#attribute.
    def exitAttribute(self, ctx:DSLParser.AttributeContext):
        pass


    # Enter a parse tree produced by DSLParser#propertyName.
    def enterPropertyName(self, ctx:DSLParser.PropertyNameContext):
        pass

    # Exit a parse tree produced by DSLParser#propertyName.
    def exitPropertyName(self, ctx:DSLParser.PropertyNameContext):
        pass


    # Enter a parse tree produced by DSLParser#value.
    def enterValue(self, ctx:DSLParser.ValueContext):
        pass

    # Exit a parse tree produced by DSLParser#value.
    def exitValue(self, ctx:DSLParser.ValueContext):
        pass


    # Enter a parse tree produced by DSLParser#identifier.
    def enterIdentifier(self, ctx:DSLParser.IdentifierContext):
        pass

    # Exit a parse tree produced by DSLParser#identifier.
    def exitIdentifier(self, ctx:DSLParser.IdentifierContext):
        pass


    # Enter a parse tree produced by DSLParser#stringLiteral.
    def enterStringLiteral(self, ctx:DSLParser.StringLiteralContext):
        pass

    # Exit a parse tree produced by DSLParser#stringLiteral.
    def exitStringLiteral(self, ctx:DSLParser.StringLiteralContext):
        pass


    # Enter a parse tree produced by DSLParser#numberLiteral.
    def enterNumberLiteral(self, ctx:DSLParser.NumberLiteralContext):
        pass

    # Exit a parse tree produced by DSLParser#numberLiteral.
    def exitNumberLiteral(self, ctx:DSLParser.NumberLiteralContext):
        pass


    # Enter a parse tree produced by DSLParser#booleanLiteral.
    def enterBooleanLiteral(self, ctx:DSLParser.BooleanLiteralContext):
        pass

    # Exit a parse tree produced by DSLParser#booleanLiteral.
    def exitBooleanLiteral(self, ctx:DSLParser.BooleanLiteralContext):
        pass


    # Enter a parse tree produced by DSLParser#valueComp.
    def enterValueComp(self, ctx:DSLParser.ValueCompContext):
        pass

    # Exit a parse tree produced by DSLParser#valueComp.
    def exitValueComp(self, ctx:DSLParser.ValueCompContext):
        pass


    # Enter a parse tree produced by DSLParser#nodeComp.
    def enterNodeComp(self, ctx:DSLParser.NodeCompContext):
        pass

    # Exit a parse tree produced by DSLParser#nodeComp.
    def exitNodeComp(self, ctx:DSLParser.NodeCompContext):
        pass



del DSLParser
package repair.dsl.kirin.map.code.node;

import repair.ast.MoNode;
import repair.ast.code.expression.literal.*;
import repair.dsl.kirin.map.code.KeyWord;
import repair.dsl.kirin.map.code.Nameable;

public class Literal extends DSLNode implements Nameable, KeyWord {
    @Override
    public String prettyPrint() {
        return "literal";
    }

    @Override
    public NameAttr getNameAttr(MoNode node) {
        if (node instanceof MoStringLiteral stringLiteral) {
            String str = stringLiteral.getValue() == null ? "" : stringLiteral.getValue();
            return new NameAttr("value", str, true);
        } else if (node instanceof MoCharacterLiteral characterLiteral) {
            return new NameAttr("value", String.valueOf(characterLiteral.getValue()), true);
        } else if (node instanceof MoBooleanLiteral booleanLiteral) {
            return new NameAttr("value", String.valueOf(booleanLiteral.getValue()), false);
        } else if (node instanceof MoNumberLiteral numberLiteral) {
            String number = handleNumberLiteralAffix(numberLiteral.getValue());
            return new NameAttr("value", number, false);
        } else if (node instanceof MoNullLiteral nullLiteral) {
            return new NameAttr("value", nullLiteral.getNullValue(), false);
        }
        throw new RuntimeException("should not happened");
    }

    private String handleNumberLiteralAffix(String value) {
        String number = value;
        final String[] PREFIXES = {"0x", "0X", "0b", "0B", "0o"};
        final String[] SUFFIXES = {"L", "l", "F", "f", "D", "d", "U", "u"};

        // 去除前缀
        for (String prefix : PREFIXES) {
            if (number.startsWith(prefix)) {
                number = number.substring(prefix.length());
                break;
            }
        }

        // 去除后缀
        for (String suffix : SUFFIXES) {
            if (number.endsWith(suffix)) {
                number = number.substring(0, number.length() - suffix.length());
                break;
            }
        }

        return number;
    }

}

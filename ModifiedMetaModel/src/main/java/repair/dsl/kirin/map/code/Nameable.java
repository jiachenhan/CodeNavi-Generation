package repair.dsl.kirin.map.code;

import repair.ast.MoNode;

public interface Nameable {

    record NameAttr(String keyName, String valueName, boolean hasQuotationMark){}

    // (name, value), "xxx", 是否有引号
    NameAttr getNameAttr(MoNode node);
}

package repair.dsl.kirin.alias;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import repair.dsl.kirin.map.code.node.DSLNode;
import repair.dsl.kirin.query.NormalQuery;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AliasManager {
    private static AliasManager instance;

    private final BidiMap<Aliasable, Alias> aliasMap;
    private final Map<Class<? extends DSLNode>, AtomicInteger> typeAliasCounter;
    private final AtomicInteger otherCounter;

    private AliasManager() {
        aliasMap = new DualHashBidiMap<>();
        typeAliasCounter = new HashMap<>();
        otherCounter = new AtomicInteger(1);
    }

    public static synchronized AliasManager getInstance() {
        if (instance == null) {
            instance = new AliasManager();
        }
        return instance;
    }

    private Alias generateAlias(Aliasable aliasable) {
        if (aliasable instanceof NormalQuery normalQuery) {
            DSLNode dslNode = normalQuery.getDslNode();
            AtomicInteger atomicInteger = typeAliasCounter.computeIfAbsent(dslNode.getClass(), k -> new AtomicInteger(1));
            String aliasKey = dslNode.prettyPrint().toLowerCase() + "_" + atomicInteger.getAndIncrement();
            return new Alias(aliasKey);
        } else {
            String aliasKey = "alias_" + otherCounter.getAndIncrement();
            return new Alias(aliasKey);
        }
    }

    public Alias getAlias(Aliasable aliasable) {
        return aliasMap.computeIfAbsent(aliasable, this::generateAlias);
    }

}

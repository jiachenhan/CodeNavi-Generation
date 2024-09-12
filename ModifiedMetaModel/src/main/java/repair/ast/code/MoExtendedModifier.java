package repair.ast.code;

import repair.ast.MoNode;
import repair.ast.code.expression.MoAnnotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface MoExtendedModifier {
    /**
     * Returns whether this extended modifier is a standard modifier.
     *
     * @return <code>true</code> if this is a standard modifier
     * (instance of {@link MoModifier}), and <code>false</code> otherwise
     */
    public boolean isModifier();

    /**
     * Returns whether this extended modifier is an annotation.
     *
     * @return <code>true</code> if this is an annotation
     * (instance of a subclass of {@link MoAnnotation}), and
     * <code>false</code> otherwise
     */
    public boolean isAnnotation();

    public static boolean sameList(List<? extends MoExtendedModifier> modifiers1, List<? extends MoExtendedModifier> modifiers2){
        if(modifiers1.size() != modifiers2.size()){
            return false;
        }
        Set<? extends MoExtendedModifier> set1 = Set.copyOf(modifiers1);
        Set<? extends MoExtendedModifier> set2 = Set.copyOf(modifiers2);

        List<? extends MoExtendedModifier> unmatched = new ArrayList<>(set2);

        for(MoExtendedModifier modifier : set1){
            boolean found = false;
            Iterator<? extends MoExtendedModifier> iterator = unmatched.iterator();
            while(iterator.hasNext()){
                MoExtendedModifier unmatchedModifier = iterator.next();
                if(modifier instanceof MoModifier modifier1){
                    if(modifier1.isSame((MoNode) unmatchedModifier)){
                        iterator.remove();
                        found = true;
                        break;
                    }
                } else if(modifier instanceof MoAnnotation annotation1){
                    if(annotation1.isSame((MoNode) unmatchedModifier)){
                        iterator.remove();
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        return unmatched.isEmpty();
    }
}

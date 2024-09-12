package repair.jls.example;

import java.util.Comparator;

public class LambdaExpressionExample {
    Comparator<Integer> VariableDeclarationFragmentComp = (a, b) -> a - b;
    Comparator<Integer> SingleVariableDeclarationComp = (Integer a, Integer b) -> a - b;
}

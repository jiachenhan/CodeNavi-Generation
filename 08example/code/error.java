import java.util.Optional;

public class foo{
    public static String check() {
        Optional<String> opt = new Optional();
        return opt.get();
    }
}
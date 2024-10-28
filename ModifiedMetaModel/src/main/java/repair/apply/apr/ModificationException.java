package repair.apply.apr;

import java.io.Serial;

public class ModificationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -599663165140437833L;

    public ModificationException(String s) {
        super(s);
    }
}

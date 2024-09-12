package repair.ast.code.context;

public record LineRange(int start, int end) {

    public boolean contains(int line) {
        return start <= line && line <= end;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LineRange range)) {
            return false;
        }

        return start == range.start && end == range.end;
    }
}

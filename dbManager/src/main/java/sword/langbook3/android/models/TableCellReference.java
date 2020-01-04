package sword.langbook3.android.models;

public final class TableCellReference {

    public final int bunchSet;
    public final int rule;

    public TableCellReference(int bunchSet, int rule) {
        this.bunchSet = bunchSet;
        this.rule = rule;
    }

    @Override
    public int hashCode() {
        return bunchSet * 31 + rule;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TableCellReference)) {
            return false;
        }

        final TableCellReference that = (TableCellReference) other;
        return bunchSet == that.bunchSet && rule == that.rule;
    }
}

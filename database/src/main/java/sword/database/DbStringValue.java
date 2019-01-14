package sword.database;

public final class DbStringValue implements DbValue {
    private final String _value;

    public DbStringValue(String value) {
        _value = value;
    }

    public String get() {
        return _value;
    }

    @Override
    public boolean isText() {
        return true;
    }

    @Override
    public int toInt() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("String column should not be converted to integer");
    }

    @Override
    public String toText() {
        return _value;
    }
}

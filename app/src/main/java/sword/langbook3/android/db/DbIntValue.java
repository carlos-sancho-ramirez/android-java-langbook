package sword.langbook3.android.db;

public final class DbIntValue implements DbValue {
    private final int _value;

    public DbIntValue(int value) {
        _value = value;
    }

    public int get() {
        return _value;
    }

    @Override
    public boolean isText() {
        return false;
    }

    @Override
    public int toInt() {
        return get();
    }

    @Override
    public String toText() {
        return Integer.toString(_value);
    }
}

package sword.langbook3.android.db;

public final class AlphabetId {
    public final int key;

    public AlphabetId(int key) {
        if (key == 0) {
            throw new IllegalArgumentException();
        }

        this.key = key;
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AlphabetId && ((AlphabetId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}

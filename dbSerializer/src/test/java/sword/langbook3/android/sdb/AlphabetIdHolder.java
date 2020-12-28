package sword.langbook3.android.sdb;

final class AlphabetIdHolder {

    final int key;

    AlphabetIdHolder(int key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof AlphabetIdHolder)) {
            return false;
        }

        return ((AlphabetIdHolder) other).key == key;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + key + ")";
    }
}

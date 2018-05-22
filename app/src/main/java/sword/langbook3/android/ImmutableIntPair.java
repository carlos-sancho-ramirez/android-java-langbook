package sword.langbook3.android;

public final class ImmutableIntPair {

    public final int left;
    public final int right;

    public ImmutableIntPair(int left, int right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int hashCode() {
        return left * 31 + right;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ImmutableIntPair)) {
            return false;
        }

        final ImmutableIntPair that = (ImmutableIntPair) other;
        return left == that.left && right == that.right;
    }
}

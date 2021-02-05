package sword.langbook3.android.models;

import sword.collections.ImmutableIntRange;

public final class SentenceSpan<AcceptationId> {
    public final ImmutableIntRange range;
    public final AcceptationId acceptation;

    public SentenceSpan(ImmutableIntRange range, AcceptationId acceptation) {
        if (range == null || range.min() < 0 || acceptation == null) {
            throw new IllegalArgumentException();
        }

        this.range = range;
        this.acceptation = acceptation;
    }

    @Override
    public int hashCode() {
        return ((range.min() * 37) + range.size() * 37) + acceptation.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        else if (!(other instanceof SentenceSpan)) {
            return false;
        }

        final SentenceSpan that = (SentenceSpan) other;
        return acceptation.equals(that.acceptation) && range.equals(that.range);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + range + ", " + acceptation + ')';
    }
}

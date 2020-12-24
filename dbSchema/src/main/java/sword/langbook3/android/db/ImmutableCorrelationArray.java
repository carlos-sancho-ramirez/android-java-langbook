package sword.langbook3.android.db;

import sword.collections.Function;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableList;
import sword.collections.IntResultFunction;
import sword.collections.Map;
import sword.collections.MutableList;
import sword.collections.Predicate;
import sword.collections.Traversable;
import sword.collections.Traverser;

public final class ImmutableCorrelationArray implements Traversable<ImmutableCorrelation> {
    private static final ImmutableCorrelationArray EMPTY = new ImmutableCorrelationArray(ImmutableList.empty());
    private final ImmutableList<ImmutableCorrelation> array;

    public static ImmutableCorrelationArray empty() {
        return EMPTY;
    }

    ImmutableCorrelationArray(ImmutableList<ImmutableCorrelation> array) {
        if (array == null) {
            throw new IllegalArgumentException();
        }

        this.array = array;
    }

    @Override
    public Traverser<ImmutableCorrelation> iterator() {
        return array.iterator();
    }

    public ImmutableCorrelationArray filter(Predicate<? super ImmutableCorrelation> predicate) {
        final ImmutableList<ImmutableCorrelation> newArray = array.filter(predicate);
        return (newArray == array)? this : (newArray == EMPTY.array)? EMPTY : new ImmutableCorrelationArray(newArray);
    }

    public <U> ImmutableList<U> map(Function<? super ImmutableCorrelation, ? extends U> func) {
        return array.map(func);
    }

    public ImmutableIntList mapToInt(IntResultFunction<? super ImmutableCorrelation> func) {
        return array.mapToInt(func);
    }

    public ImmutableCorrelationArray prepend(ImmutableCorrelation item) {
        final ImmutableList<ImmutableCorrelation> newArray = array.prepend(item);
        return (newArray == array)? this : (newArray == EMPTY.array)? EMPTY : new ImmutableCorrelationArray(newArray);
    }

    public ImmutableCorrelation concatenateTexts() {
        return reduce((corr1, corr2) -> {
            final MutableCorrelation mixed = corr1.mutate();
            for (Map.Entry<AlphabetId, String> entry : corr2.entries()) {
                final AlphabetId key = entry.key();
                mixed.put(key, mixed.get(key) + entry.value());
            }

            return mixed.toImmutable();
        });
    }

    @Override
    public int hashCode() {
        return array.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ImmutableCorrelationArray)) {
            return false;
        }

        final ImmutableCorrelationArray that = (ImmutableCorrelationArray) other;
        return array.equals(that.array);
    }

    public static final class Builder {
        private final MutableList<ImmutableCorrelation> array = MutableList.empty();

        public Builder append(ImmutableCorrelation correlation) {
            if (correlation == null) {
                throw new IllegalArgumentException();
            }

            array.append(correlation);
            return this;
        }

        public Builder add(ImmutableCorrelation correlation) {
            return append(correlation);
        }

        public ImmutableCorrelationArray build() {
            return new ImmutableCorrelationArray(array.toImmutable());
        }
    }
}

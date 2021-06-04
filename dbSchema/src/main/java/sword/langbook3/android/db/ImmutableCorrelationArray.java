package sword.langbook3.android.db;

import sword.collections.Function;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.IntResultFunction;
import sword.collections.Map;
import sword.collections.MutableList;
import sword.collections.Predicate;
import sword.collections.Traversable;
import sword.collections.Traverser;

public final class ImmutableCorrelationArray<AlphabetId> implements Traversable<ImmutableCorrelation<AlphabetId>> {
    private static final ImmutableCorrelationArray EMPTY = new ImmutableCorrelationArray(ImmutableList.empty());
    private final ImmutableList<ImmutableCorrelation<AlphabetId>> array;

    public static <T> ImmutableCorrelationArray<T> empty() {
        return EMPTY;
    }

    ImmutableCorrelationArray(ImmutableList<ImmutableCorrelation<AlphabetId>> array) {
        if (array == null) {
            throw new IllegalArgumentException();
        }

        this.array = array;
    }

    @Override
    public Traverser<ImmutableCorrelation<AlphabetId>> iterator() {
        return array.iterator();
    }

    public ImmutableIntSet indexes() {
        return array.indexes();
    }

    public ImmutableCorrelationArray<AlphabetId> filter(Predicate<? super ImmutableCorrelation<AlphabetId>> predicate) {
        final ImmutableList<ImmutableCorrelation<AlphabetId>> newArray = array.filter(predicate);
        return (newArray == array)? this : (newArray == EMPTY.array)? empty() : new ImmutableCorrelationArray<>(newArray);
    }

    public <U> ImmutableList<U> map(Function<? super ImmutableCorrelation<AlphabetId>, ? extends U> func) {
        return array.map(func);
    }

    public ImmutableIntList mapToInt(IntResultFunction<? super ImmutableCorrelation<AlphabetId>> func) {
        return array.mapToInt(func);
    }

    public ImmutableCorrelationArray<AlphabetId> prepend(ImmutableCorrelation<AlphabetId> item) {
        final ImmutableList<ImmutableCorrelation<AlphabetId>> newArray = array.prepend(item);
        return (newArray == array)? this : (newArray == EMPTY.array)? empty() : new ImmutableCorrelationArray<>(newArray);
    }

    public ImmutableCorrelation<AlphabetId> concatenateTexts() {
        if (array.isEmpty()) {
            return ImmutableCorrelation.empty();
        }

        return reduce((corr1, corr2) -> {
            final MutableCorrelation<AlphabetId> mixed = corr1.mutate();
            for (Map.Entry<AlphabetId, String> entry : corr2.entries()) {
                final AlphabetId key = entry.key();
                mixed.put(key, mixed.get(key) + entry.value());
            }

            return mixed.toImmutable();
        });
    }

    public ImmutableCorrelationArray<AlphabetId> reverse() {
        final ImmutableList<ImmutableCorrelation<AlphabetId>> newArray = array.reverse();
        return (array == newArray)? this : new ImmutableCorrelationArray<>(newArray);
    }

    public ImmutableList<ImmutableCorrelation<AlphabetId>> toList() {
        return array;
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

    public static final class Builder<AlphabetId> {
        private final MutableList<ImmutableCorrelation<AlphabetId>> array = MutableList.empty();

        public Builder<AlphabetId> append(ImmutableCorrelation<AlphabetId> correlation) {
            if (correlation == null) {
                throw new IllegalArgumentException();
            }

            array.append(correlation);
            return this;
        }

        public Builder<AlphabetId> add(ImmutableCorrelation<AlphabetId> correlation) {
            return append(correlation);
        }

        public ImmutableCorrelationArray<AlphabetId> build() {
            return new ImmutableCorrelationArray<>(array.toImmutable());
        }
    }
}

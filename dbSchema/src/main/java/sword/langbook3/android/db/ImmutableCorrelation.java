package sword.langbook3.android.db;

import java.util.Iterator;

import sword.collections.ArrayLengthFunction;
import sword.collections.Function;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntResultFunction;
import sword.collections.Map;
import sword.collections.Predicate;
import sword.collections.SortFunction;
import sword.collections.TransformerWithKey;
import sword.collections.UnmappedKeyException;

public final class ImmutableCorrelation<AlphabetId> implements Correlation<AlphabetId>, ImmutableMap<AlphabetId, String> {

    private static final ImmutableCorrelation EMPTY = new ImmutableCorrelation(ImmutableHashMap.empty());
    private final ImmutableMap<AlphabetId, String> map;

    ImmutableCorrelation(ImmutableMap<AlphabetId, String> map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        this.map = map;
    }

    public static <T> ImmutableCorrelation<T> empty() {
        return EMPTY;
    }

    @Override
    public int indexOfKey(AlphabetId key) {
        return map.indexOfKey(key);
    }

    @Override
    public String get(AlphabetId key) throws UnmappedKeyException {
        return map.get(key);
    }

    @Override
    public String get(AlphabetId key, String defaultValue) {
        return map.get(key, defaultValue);
    }

    @Override
    public TransformerWithKey<AlphabetId, String> iterator() {
        return map.iterator();
    }

    @Override
    public AlphabetId keyAt(int index) {
        return map.keyAt(index);
    }

    @Override
    public ImmutableSet<AlphabetId> keySet() {
        return map.keySet();
    }

    @Override
    public ImmutableSet<Map.Entry<AlphabetId, String>> entries() {
        return map.entries();
    }

    public ImmutableCorrelation<AlphabetId> put(AlphabetId key, String value) {
        final ImmutableMap<AlphabetId, String> newMap = map.put(key, value);
        return (newMap == map)? this : new ImmutableCorrelation<>(newMap);
    }

    @Override
    public ImmutableList<String> toList() {
        return map.toList();
    }

    @Override
    public ImmutableSet<String> toSet() {
        return map.toSet();
    }

    @Override
    public ImmutableIntSet indexes() {
        return map.indexes();
    }

    @Override
    public ImmutableIntValueMap<String> count() {
        return map.count();
    }

    @Override
    public ImmutableCorrelation<AlphabetId> filter(Predicate<? super String> predicate) {
        final ImmutableMap<AlphabetId, String> newMap = map.filter(predicate);
        return (newMap == map)? this : new ImmutableCorrelation<>(newMap);
    }

    @Override
    public ImmutableCorrelation<AlphabetId> filterNot(Predicate<? super String> predicate) {
        final ImmutableMap<AlphabetId, String> newMap = map.filterNot(predicate);
        return (newMap == map)? this : new ImmutableCorrelation<>(newMap);
    }

    @Override
    public ImmutableIntValueMap<AlphabetId> mapToInt(IntResultFunction<? super String> mapFunc) {
        return map.mapToInt(mapFunc);
    }

    @Override
    public ImmutableCorrelation<AlphabetId> toImmutable() {
        return this;
    }

    @Override
    public MutableCorrelation<AlphabetId> mutate() {
        return new MutableCorrelation<>(map.mutate());
    }

    @Override
    public MutableCorrelation<AlphabetId> mutate(ArrayLengthFunction arrayLengthFunction) {
        return new MutableCorrelation<>(map.mutate(arrayLengthFunction));
    }

    @Override
    public <U> ImmutableMap<AlphabetId, U> map(Function<? super String, ? extends U> mapFunc) {
        return map.map(mapFunc);
    }

    @Override
    public ImmutableMap<AlphabetId, String> sort(SortFunction<? super AlphabetId> function) {
        return map.sort(function);
    }

    public ImmutableCorrelation<AlphabetId> removeAt(int index) {
        return new ImmutableCorrelation<>(map.removeAt(index));
    }

    private static <AlphabetId> boolean entryLessThan(SortFunction<? super AlphabetId> alphabetIdComparator, ImmutableCorrelationArray<AlphabetId> a, ImmutableCorrelationArray<AlphabetId> b) {
        final Iterator<ImmutableCorrelation<AlphabetId>> itA = a.iterator();
        final Iterator<ImmutableCorrelation<AlphabetId>> itB = b.iterator();

        while (itA.hasNext() && itB.hasNext()) {
            ImmutableCorrelation<AlphabetId> headA = itA.next();
            ImmutableCorrelation<AlphabetId> headB = itB.next();

            for (int i = 0; i < headA.size(); i++) {
                final AlphabetId alphabet = headA.keyAt(i);
                if (headB.size() == i) {
                    return false;
                }

                final AlphabetId alphabetB = headB.keyAt(i);

                if (alphabetIdComparator.lessThan(alphabet, alphabetB)) {
                    return true;
                }
                else if (alphabetIdComparator.lessThan(alphabetB, alphabet)) {
                    return false;
                }

                final String textA = headA.valueAt(i);
                final String textB = headB.valueAt(i);
                if (textA.length() < textB.length()) {
                    return true;
                }
                else if (textA.length() > textB.length()) {
                    return false;
                }
            }
        }

        return itB.hasNext();
    }

    private static <AlphabetId> SortFunction<ImmutableCorrelationArray<AlphabetId>> entryLessThanFunction(SortFunction<? super AlphabetId> alphabetIdComparator) {
        return (a, b) -> entryLessThan(alphabetIdComparator, a, b);
    }

    private void checkPossibleCorrelationArraysRecursive(
            SortFunction<? super AlphabetId> alphabetIdComparator,
            ImmutableSet.Builder<ImmutableCorrelationArray<AlphabetId>> builder,
            ImmutableCorrelation<AlphabetId> left,
            ImmutableCorrelation<AlphabetId> right) {
        final int remainingSize = size();
        if (remainingSize == 0) {
            for (ImmutableCorrelationArray<AlphabetId> array : right.checkPossibleCorrelationArrays(alphabetIdComparator)) {
                builder.add(array.prepend(left));
            }
        }
        else {
            final AlphabetId firstAlphabet = keyAt(0);
            final String firstText = valueAt(0);

            // TODO: Change this to global.skip(1) when available
            final ImmutableCorrelation.Builder<AlphabetId> tailBuilder = new ImmutableCorrelation.Builder<>();
            for (int i = 1; i < remainingSize; i++) {
                tailBuilder.put(keyAt(i), valueAt(i));
            }
            final ImmutableCorrelation<AlphabetId> tail = tailBuilder.build();

            final int firstTextSize = firstText.length();
            for (int i = 1; i < firstTextSize; i++) {
                final ImmutableCorrelation<AlphabetId> newLeft = left.put(firstAlphabet, firstText.substring(0, i));
                final ImmutableCorrelation<AlphabetId> newRight = right.put(firstAlphabet, firstText.substring(i));
                tail.checkPossibleCorrelationArraysRecursive(alphabetIdComparator, builder, newLeft, newRight);
            }
        }
    }

    public ImmutableSet<ImmutableCorrelationArray<AlphabetId>> checkPossibleCorrelationArrays(SortFunction<? super AlphabetId> alphabetIdComparator) {
        final int globalSize = size();
        final IntResultFunction<String> lengthFunc = text -> (text == null)? 0 : text.length();
        final ImmutableIntValueMap<AlphabetId> lengths = mapToInt(lengthFunc);
        if (globalSize == 0 || lengths.anyMatch(length -> length <= 0)) {
            return ImmutableHashSet.empty();
        }

        final ImmutableSet.Builder<ImmutableCorrelationArray<AlphabetId>> builder = new ImmutableHashSet.Builder<>();
        builder.add(new ImmutableCorrelationArray.Builder<AlphabetId>().add(this).build());

        if (globalSize > 1) {
            checkPossibleCorrelationArraysRecursive(alphabetIdComparator, builder, ImmutableCorrelation.empty(), ImmutableCorrelation.empty());
        }
        return builder.build().sort(entryLessThanFunction(alphabetIdComparator));
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ImmutableCorrelation)) {
            return false;
        }

        final ImmutableCorrelation that = (ImmutableCorrelation) other;
        return map.equals(that.map);
    }

    public static final class Builder<AlphabetId> {
        private final MutableCorrelation<AlphabetId> correlation = MutableCorrelation.empty();

        public Builder<AlphabetId> put(AlphabetId alphabet, String text) {
            correlation.put(alphabet, text);
            return this;
        }

        public ImmutableCorrelation<AlphabetId> build() {
            return correlation.toImmutable();
        }
    }
}
